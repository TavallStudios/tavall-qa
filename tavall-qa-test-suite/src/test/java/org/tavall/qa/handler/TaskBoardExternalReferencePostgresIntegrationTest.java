package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.request.TaskBoardTaskUpsertRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceIngestRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceIngestRequest.Observation;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictResolution;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationOutcome;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;
import org.tavall.qa.handler.ITaskBoardExternalReferenceHandler;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExternalReferenceDataHandler;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Owner-side reference acceptance through mapped JPA and the isolated native PostgreSQL fixture. */
@Execution(ExecutionMode.SAME_THREAD)
class TaskBoardExternalReferencePostgresIntegrationTest {
    @TempDir static Path directory;
    private static TaskBoardPostgresFixture postgres;
    private final List<TaskBoardPostgresComposition> compositions = new ArrayList<>();

    @BeforeAll
    static void start() throws Exception {
        postgres = new TaskBoardPostgresFixture(directory);
    }

    @AfterAll
    static void stop() throws Exception {
        if (postgres != null) postgres.close();
    }

    @AfterEach
    void closeCompositions() {
        compositions.forEach(TaskBoardPostgresComposition::close);
        compositions.clear();
    }

    @Test
    void concurrentSameIdentityAndExactReplayCreateOneReferenceAndAudit() throws Exception {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardPostgresComposition composition = composition();
        ITaskBoardDataHandler tasks = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        ITaskBoardExternalReferenceHandler references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        TaskBoardTask task = createTask(mutations, subjects, "Concurrent external reference");
        var request = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24001", "88001", "acme/board#41", "https://github.com/acme/board/issues/41",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "cursor-1", "issue-open", 0);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = workers.submit(() -> submit(references, subjects, request, ready, start));
            var second = workers.submit(() -> submit(references, subjects, request, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (firstResult.outcome() == TaskBoardExternalReferenceMutationOutcome.CREATED ? 1 : 0)
                    + (secondResult.outcome() == TaskBoardExternalReferenceMutationOutcome.CREATED ? 1 : 0));
            assertTrue(firstResult.reference().orElseThrow().referenceId()
                    .equals(secondResult.reference().orElseThrow().referenceId()));
        }

        var replay = references.registerOrReconcile(subjects, request);
        assertEquals(TaskBoardExternalReferenceMutationOutcome.REPLAYED, replay.outcome());
        assertEquals(1, references.findReferencesForTask(subjects, task.taskId()).size());
        assertEquals(1, tasks.findAuditEntries(task.taskId()).stream()
                .filter(entry -> entry.eventType() == TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED).count());
    }

    @Test
    void tj224MirrorClosureQueuesConflictAndKeepsLinearAndQaExecutionState() {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardPostgresComposition composition = composition();
        ITaskBoardDataHandler tasks = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        ITaskBoardExternalReferenceHandler references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        TaskBoardTask task = createTask(mutations, subjects, "TJ-224 implementation task");

        var linear = observation(task.taskId(), TaskBoardExternalReferenceProvider.LINEAR_ISSUE,
                "00000000-0000-0000-0000-000000022244", "00000000-0000-0000-0000-000000022400",
                "TJ-224", "https://linear.app/tavall/issue/TJ-224",
                TaskBoardExternalReferenceSourceStatus.ACTIVE, "linear-rev-1", "linear-cursor-1", "linear-active", 0);
        references.registerOrReconcile(subjects, linear);

        var githubOpen = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24002", "88002", "acme/board#42", "https://github.com/acme/board/issues/42",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "github-cursor-1", "github-open", 0);
        references.registerOrReconcile(subjects, githubOpen);

        TaskBoardTask beforeClose = tasks.findTask(task.taskId()).orElseThrow();
        var githubClosed = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24002", "88002", "acme/board#42", "https://github.com/acme/board/issues/42",
                TaskBoardExternalReferenceSourceStatus.CLOSED, "gh-node-2", "github-cursor-2", "github-closed", 1);
        var firstConflict = references.registerOrReconcile(subjects, githubClosed);
        var repeatedConflict = references.registerOrReconcile(subjects, githubClosed);

        assertEquals(TaskBoardExternalReferenceMutationOutcome.CONFLICT, firstConflict.outcome());
        assertEquals(firstConflict.conflict().orElseThrow().conflictId(), repeatedConflict.conflict().orElseThrow().conflictId());
        assertEquals(1, references.findOpenConflicts(subjects, java.util.Optional.of(task.taskId())).size());
        assertEquals(TaskBoardExternalReferenceSourceStatus.OPEN, firstConflict.reference().orElseThrow().sourceStatus());
        assertEquals(TaskBoardExternalReferenceSourceStatus.ACTIVE, references.findReference(subjects, linear.key()).orElseThrow().sourceStatus());

        TaskBoardTask afterClose = tasks.findTask(task.taskId()).orElseThrow();
        assertQaStateUnchanged(beforeClose, afterClose);
        assertEquals(1, tasks.findAuditEntries(task.taskId()).stream()
                .filter(entry -> entry.eventType() == TaskBoardAuditEventType.EXTERNAL_REFERENCE_CONFLICTED).count());

        var conflict = firstConflict.conflict().orElseThrow();
        var resolved = references.resolveConflict(subjects,
                new TaskBoardExternalReferenceConflictResolutionRequest(conflict.conflictId(),
                        TaskBoardExternalReferenceConflictResolution.KEEP_CURRENT,
                        firstConflict.reference().orElseThrow().syncRevision(), "Keep active Linear record authoritative"));
        assertTrue(resolved.resolvedAt().isPresent());
        assertEquals(TaskBoardExternalReferenceSourceStatus.OPEN, references.findReference(subjects, githubOpen.key())
                .orElseThrow().sourceStatus());
        assertQaStateUnchanged(beforeClose, tasks.findTask(task.taskId()).orElseThrow());
    }

    @Test
    void staleReferenceRevisionCreatesOneDurableConflictWithoutReplacingAcceptedFields() {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardPostgresComposition composition = composition();
        ITaskBoardDataHandler tasks = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        ITaskBoardExternalReferenceHandler references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        TaskBoardTask task = createTask(mutations, subjects, "Stale reference revision");
        var initial = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24003", "88003", "acme/board#43", "https://github.com/acme/board/issues/43",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "cursor-1", "issue-open", 0);
        references.registerOrReconcile(subjects, initial);

        var stale = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24003", "88003", "acme/board#43", "https://github.com/acme/board/issues/43",
                TaskBoardExternalReferenceSourceStatus.BLOCKED, "gh-node-2", "cursor-2", "issue-blocked", 0);
        var first = references.registerOrReconcile(subjects, stale);
        var retry = references.registerOrReconcile(subjects, stale);

        assertEquals(TaskBoardExternalReferenceMutationOutcome.CONFLICT, first.outcome());
        assertEquals(first.conflict().orElseThrow().conflictId(), retry.conflict().orElseThrow().conflictId());
        assertEquals(first.reference().orElseThrow().syncRevision(), retry.reference().orElseThrow().syncRevision());
        assertEquals("gh-node-1", first.reference().orElseThrow().lastAppliedRevision());
        assertEquals(TaskBoardExternalReferenceSourceStatus.OPEN, first.reference().orElseThrow().sourceStatus());
        assertEquals(1, references.findOpenConflicts(subjects, java.util.Optional.of(task.taskId())).size());
        assertEquals(1, tasks.findAuditEntries(task.taskId()).stream()
                .filter(entry -> entry.eventType() == TaskBoardAuditEventType.EXTERNAL_REFERENCE_CONFLICTED).count());
    }

    @Test
    void aSecondProviderIdentityForOneTaskIsQueuedInsteadOfViolatingTheOneToOneConstraint() {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardPostgresComposition composition = composition();
        ITaskBoardDataHandler tasks = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        ITaskBoardExternalReferenceHandler references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        TaskBoardTask task = createTask(mutations, subjects, "Provider slot conflict");
        var first = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24004", "88004", "acme/board#44", "https://github.com/acme/board/issues/44",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "cursor-1", "issue-44", 0);
        references.registerOrReconcile(subjects, first);

        var duplicateSlot = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24004", "88005", "acme/board#45", "https://github.com/acme/board/issues/45",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "cursor-2", "issue-45", 0);
        var result = references.registerOrReconcile(subjects, duplicateSlot);

        assertEquals(TaskBoardExternalReferenceMutationOutcome.CONFLICT, result.outcome());
        assertTrue(result.reference().isEmpty());
        assertEquals(1, references.findReferencesForTask(subjects, task.taskId()).size());
        assertEquals(1, references.findOpenConflicts(subjects, java.util.Optional.of(task.taskId())).size());
    }

    @Test
    void dependencyMapSwapVisibilityWithoutReconstructingExternalReferenceHandler() {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardPostgresComposition composition = composition();
        var dependencies = composition.dependencies();
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        ITaskBoardExternalReferenceHandler references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        TaskBoardTask task = createTask(mutations, subjects, "Swap visibility external reference");
        var initial = observation(task.taskId(), TaskBoardExternalReferenceProvider.GITHUB_ISSUE,
                "24005", "88005", "acme/board#46", "https://github.com/acme/board/issues/46",
                TaskBoardExternalReferenceSourceStatus.OPEN, "gh-node-1", "cursor-1", "issue-46", 0);
        var created = references.registerOrReconcile(subjects, initial);
        assertEquals(TaskBoardExternalReferenceMutationOutcome.CREATED, created.outcome());

        // The composition boundary registered the handler and its generated access; a swapped
        // external-reference generation must be visible to the same live handler instance.
        assertSame(references, dependencies.findInstance(ITaskBoardExternalReferenceHandler.class));
        assertSame(references, composition.bean(TaskBoardExternalReferenceHandler.class));
        assertNotNull(dependencies.findInstance(TaskBoardExternalReferenceHandlerDependencyAccess.class));
        dependencies.replaceInstance(ITaskBoardExternalReferenceDataHandler.class,
                () -> (ITaskBoardExternalReferenceDataHandler) Proxy.newProxyInstance(
                        ITaskBoardExternalReferenceDataHandler.class.getClassLoader(),
                        new Class<?>[]{ITaskBoardExternalReferenceDataHandler.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "isAvailable" -> true;
                            case "findReference", "findConflict" -> Optional.empty();
                            case "findReferencesForTask", "findOpenConflicts" -> List.of();
                            case "toString" -> "qa25-swapped-external-reference-generation";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        }));
        assertSame(references, composition.bean(ITaskBoardExternalReferenceHandler.class),
                "Generation replacement must not reconstruct the handler");
        var swappedConflict = new TaskBoardExternalReferenceConflictResolutionRequest(UUID.randomUUID(),
                TaskBoardExternalReferenceConflictResolution.KEEP_CURRENT, 0, "Swapped generation has no conflicts");
        assertThrows(IllegalArgumentException.class, () -> references.resolveConflict(subjects, swappedConflict));
        assertTrue(references.findReference(subjects, initial.key()).isEmpty());

        // Swapping the real generation back restores reconciliation and exact replay on the same instance.
        dependencies.replaceInstance(ITaskBoardExternalReferenceDataHandler.class,
                () -> composition.bean(ITaskBoardExternalReferenceDataHandler.class));
        var replay = references.registerOrReconcile(subjects, initial);
        assertEquals(TaskBoardExternalReferenceMutationOutcome.REPLAYED, replay.outcome());
        assertEquals(created.reference().orElseThrow().referenceId(), replay.reference().orElseThrow().referenceId());
        assertEquals(1, references.findReferencesForTask(subjects, task.taskId()).size());
    }

    @Test
    void ownerIngestCreatesOneTaskAcrossConcurrentSubmissionsReplayAndRestart() throws Exception {
        var subject = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        var composition = composition();
        var references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        String project = createProject(composition.bean(ITaskBoardMutationHandler.class), subject);
        var github = github("25101", "71", "owner/original", TaskBoardExternalReferenceSourceStatus.OPEN, "gh1", 0);
        var request = ingest(project, github, Optional.of(linear("176", "TJ-176")));
        UUID id;

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = workers.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return references.ingest(subject, request);
            });
            var second = workers.submit(() -> {
                ready.countDown();
                assertTrue(start.await(10, TimeUnit.SECONDS));
                return references.ingest(subject, request);
            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            id = firstResult.taskId().orElseThrow();
            assertEquals(Optional.of(id), secondResult.taskId());
            assertTrue(firstResult.createdTask() ^ secondResult.createdTask());
        }
        assertEquals(2, references.findReferencesForTask(subject, id).size());
        assertEquals("gh1-cursor", references.findReference(subject, github.key()).orElseThrow().lastObservedCursor());
        closeCompositions();
        postgres.restart();
        var reopened = composition().bean(ITaskBoardExternalReferenceHandler.class);
        var replay = reopened.ingest(subject, request);
        assertEquals(Optional.of(id), replay.taskId());
        assertTrue(replay.observations().stream()
                .allMatch(result -> result.outcome() == TaskBoardExternalReferenceMutationOutcome.REPLAYED));
    }

    @Test
    void competingEditsAtTheSameSyncRevisionApplyOnlyOneObservation() throws Exception {
        var subject = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        var composition = composition();
        var references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        String project = createProject(composition.bean(ITaskBoardMutationHandler.class), subject);
        var first = github("25105", "75", "owner/board", TaskBoardExternalReferenceSourceStatus.OPEN, "gh1", 0);
        UUID taskId = references.ingest(subject, ingest(project, first, Optional.empty())).taskId().orElseThrow();
        var editA = github("25105", "75", "owner/board", TaskBoardExternalReferenceSourceStatus.ACTIVE, "gh2", 1);
        var editB = github("25105", "75", "owner/board", TaskBoardExternalReferenceSourceStatus.BLOCKED, "gh3", 1);

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = workers.submit(() -> submit(references, subject, editA.forTask(taskId), ready, start));
            var b = workers.submit(() -> submit(references, subject, editB.forTask(taskId), ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var outcomes = List.of(a.get(15, TimeUnit.SECONDS).outcome(), b.get(15, TimeUnit.SECONDS).outcome());
            assertTrue(outcomes.contains(TaskBoardExternalReferenceMutationOutcome.UPDATED));
            assertTrue(outcomes.contains(TaskBoardExternalReferenceMutationOutcome.CONFLICT));
        }
        var accepted = references.findReference(subject, first.key()).orElseThrow();
        assertEquals(3, accepted.syncRevision());
        assertTrue(List.of("gh2", "gh3").contains(accepted.lastAppliedRevision()));
        assertEquals(1, references.findConflictHistory(subject, taskId).size());
    }

    @Test
    void renameAdoptsLegacyTaskAndKeepsBothAliasesWhileLegacyClosurePreservesTask() {
        var subject = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        var composition = composition();
        var references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        var mutations = composition.bean(ITaskBoardMutationHandler.class);
        var tasks = composition.bean(ITaskBoardDataHandler.class);
        String project = createProject(mutations, subject);
        var input = new TaskBoardTaskCreateRequest(project, "reconciliation", "QA execution", "", "", "",
                TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, false, "", "", 0);
        var active = mutations.upsertTask(subject, new TaskBoardTaskUpsertRequest(
                "github/Owner/Legacy/issues/72", input, TaskBoardTaskStatus.ACTIVE)).task();
        var beforeIngest = tasks.findTask(active.taskId()).orElseThrow();
        var original = github("25102", "72", "owner/legacy", TaskBoardExternalReferenceSourceStatus.OPEN, "gh1", 0);
        var linear = linear("224", "TJ-224");
        references.registerOrReconcile(subject, linear.forTask(active.taskId()));
        var earlyClosure = mutations.upsertTask(subject, new TaskBoardTaskUpsertRequest(
                "github/Owner/Legacy/issues/72", input, TaskBoardTaskStatus.CANCELLED));
        assertEquals(beforeIngest, earlyClosure.task());
        assertEquals(Optional.of(active.taskId()), references.ingest(subject,
                ingest(project, original, Optional.of(linear))).taskId());

        var renamed = github("25102", "72", "owner/renamed", TaskBoardExternalReferenceSourceStatus.OPEN, "gh2", 1);
        assertEquals(Optional.of(active.taskId()), references.ingest(subject, ingest(project, renamed, Optional.empty())).taskId());
        assertEquals(List.of("github/owner/legacy/issues/72", "github/owner/renamed/issues/72"),
                references.findGitHubAliases(subject, renamed.key()));
        assertEquals(active.taskId(), references.findGitHubReference(subject, "github:OWNER/legacy#72").orElseThrow().taskId());
        assertEquals(renamed.sourceUrl(), references.findGitHubReference(subject, renamed.sourceUrl()).orElseThrow().sourceUrl());

        var closed = github("25102", "72", "owner/renamed", TaskBoardExternalReferenceSourceStatus.CLOSED, "gh3", 2);
        var conflict = references.ingest(subject, ingest(project, closed, Optional.of(linear)));
        assertEquals(Optional.of(active.taskId()), conflict.taskId());
        assertEquals(TaskBoardExternalReferenceMutationOutcome.CONFLICT, conflict.observations().getFirst().outcome());
        assertEquals("gh3-cursor", references.findReference(subject, renamed.key()).orElseThrow().lastObservedCursor());
        assertEquals(TaskBoardExternalReferenceSourceStatus.ACTIVE, references.findReference(subject, linear.key()).orElseThrow().sourceStatus());
        for (String name : List.of("Legacy", "Renamed")) {
            var result = mutations.upsertTask(subject, new TaskBoardTaskUpsertRequest(
                    "github/Owner/" + name + "/issues/72", input, TaskBoardTaskStatus.CANCELLED));
            assertEquals(active.taskId(), result.task().taskId());
        }
        assertEquals(beforeIngest, tasks.findTask(active.taskId()).orElseThrow());
        assertEquals(1, tasks.findTasks(project).size());
    }

    @Test
    void independentlyMappedSourcesQueueAmbiguityWithoutMovingEitherLink() {
        var subject = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        var composition = composition();
        var references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        var mutations = composition.bean(ITaskBoardMutationHandler.class);
        var githubTask = createTask(mutations, subject, "First task");
        var linearTask = createTask(mutations, subject, "Second task");
        var github = github("25103", "73", "owner/board", TaskBoardExternalReferenceSourceStatus.OPEN, "gh1", 0);
        var linear = linear("225", "TJ-225");
        references.registerOrReconcile(subject, github.forTask(githubTask.taskId()));
        references.registerOrReconcile(subject, linear.forTask(linearTask.taskId()));

        var request = ingest(githubTask.projectKey(), github, Optional.of(linear));
        var first = references.ingest(subject, request);
        var replay = references.ingest(subject, request);
        assertTrue(first.taskId().isEmpty());
        assertEquals(first, replay);
        var conflict = first.observations().getFirst().conflict().orElseThrow();
        assertEquals(Optional.of(linearTask.taskId()), conflict.existingTaskId());
        assertEquals(linear.key(), conflict.key());
        assertEquals(1, references.findOpenConflicts(subject, Optional.of(githubTask.taskId())).size());
        assertEquals(githubTask.taskId(), references.findReference(subject, github.key()).orElseThrow().taskId());
        assertEquals(linearTask.taskId(), references.findReference(subject, linear.key()).orElseThrow().taskId());
        assertThrows(org.tavall.qa.domain.TaskBoardConcurrentModificationException.class, () ->
                references.resolveConflict(subject, new TaskBoardExternalReferenceConflictResolutionRequest(conflict.conflictId(),
                        TaskBoardExternalReferenceConflictResolution.APPLY_INCOMING, 1, "Unsafe rebind")));
        var resolved = references.resolveConflict(subject,
                new TaskBoardExternalReferenceConflictResolutionRequest(conflict.conflictId(),
                        TaskBoardExternalReferenceConflictResolution.KEEP_CURRENT, 1, "Keep both QA tasks"));
        assertEquals(List.of(resolved.conflictId()), references.findConflictHistory(subject, linearTask.taskId())
                .stream().map(value -> value.conflictId()).toList());
        assertTrue(references.findOpenConflicts(subject, Optional.of(githubTask.taskId())).isEmpty());
    }

    @Test
    void requestWithoutAnActiveAccountGrantCannotIngestOrReadSourceMaps() {
        var composition = composition();
        var subject = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        var references = composition.bean(ITaskBoardExternalReferenceHandler.class);
        String project = createProject(composition.bean(ITaskBoardMutationHandler.class), subject);
        var github = github("25104", "74", "owner/board", TaskBoardExternalReferenceSourceStatus.OPEN, "gh1", 0);
        var revoked = new QaSubjectContext(subject.accountId(), subject.status(), subject.linkedMinecraftUuid(),
                subject.organizationId(), true, false, java.util.Set.of(), Optional.empty(), java.util.Set.of(),
                java.util.Set.of(), List.of(), false);
        for (var denied : List.of(QaSubjectContext.anonymous(), revoked)) {
            assertThrows(SecurityException.class, () -> references.ingest(denied, ingest(project, github, Optional.empty())));
            assertThrows(SecurityException.class, () -> references.findGitHubReference(denied, github.sourceUrl()));
            assertThrows(SecurityException.class, () -> references.findGitHubAliases(denied, github.key()));
            assertThrows(SecurityException.class, () -> references.findConflictHistory(denied, UUID.randomUUID()));
        }
        assertTrue(references.findReference(subject, github.key()).isEmpty());
    }

    private static TaskBoardExternalReferenceIngestRequest ingest(String project, Observation github, Optional<Observation> linear) {
        return new TaskBoardExternalReferenceIngestRequest(project, "reconciliation", "Initial source title",
                Optional.empty(), github, linear);
    }

    private static Observation github(String repositoryId, String number, String repository,
            TaskBoardExternalReferenceSourceStatus status, String revision, long expectedRevision) {
        return new Observation(new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.GITHUB_ISSUE, repositoryId, number),
                repository + "#" + number, "https://github.com/" + repository + "/issues/" + number, status,
                revision, revision + "-cursor", fingerprint(revision), Instant.parse("2026-11-04T20:00:00Z"), expectedRevision);
    }

    private static Observation linear(String idSuffix, String displayKey) {
        return new Observation(new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.LINEAR_ISSUE,
                "00000000-0000-0000-0000-000000022244", "00000000-0000-0000-0000-000000000" + idSuffix),
                displayKey, "https://linear.app/tavall/issue/" + displayKey, TaskBoardExternalReferenceSourceStatus.ACTIVE,
                "linear1", "linear1-cursor", fingerprint(displayKey), Instant.parse("2026-11-04T20:00:00Z"), 0);
    }

    private static TaskBoardExternalReferenceMutationResult submit(
            ITaskBoardExternalReferenceHandler references,
            org.tavall.qa.domain.auth.QaSubjectContext subject,
            TaskBoardExternalReferenceMutationRequest request,
            CountDownLatch ready,
            CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent reference start timed out");
        return references.registerOrReconcile(subject, request);
    }

    private TaskBoardPostgresComposition composition() {
        TaskBoardPostgresComposition composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC());
        compositions.add(composition);
        return composition;
    }

    private static TaskBoardTask createTask(ITaskBoardMutationHandler mutations,
            org.tavall.qa.domain.auth.QaSubjectContext subject, String title) {
        String projectKey = createProject(mutations, subject);
        return mutations.createTask(subject, new TaskBoardTaskCreateRequest(projectKey, "reconciliation", title, title,
                "QA execution identity is separate from external source state", "External reference integration task",
                TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, false, "test", "source://" + projectKey, 0));
    }

    private static String createProject(ITaskBoardMutationHandler mutations, QaSubjectContext subject) {
        String project = "extref-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        mutations.createProject(subject, new TaskBoardProjectCreateRequest(project, "External reference tests",
                "Isolated database integration", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0));
        return project;
    }

    private static TaskBoardExternalReferenceMutationRequest observation(UUID taskId,
            TaskBoardExternalReferenceProvider provider, String namespaceId, String sourceId, String displayKey,
            String url, TaskBoardExternalReferenceSourceStatus status, String revision, String cursor,
            String fingerprintContent, long expectedSyncRevision) {
        return new TaskBoardExternalReferenceMutationRequest(taskId,
                new org.tavall.qa.domain.external.TaskBoardExternalReferenceKey(provider, namespaceId, sourceId),
                displayKey, url, status, revision, cursor, fingerprint(fingerprintContent),
                Instant.parse("2026-11-04T20:00:00Z"), expectedSyncRevision);
    }

    private static String fingerprint(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    private static void assertQaStateUnchanged(TaskBoardTask expected, TaskBoardTask actual) {
        assertEquals(expected.taskId(), actual.taskId());
        assertEquals(expected.status(), actual.status());
        assertEquals(expected.qaStatus(), actual.qaStatus());
        assertEquals(expected.qaRevision(), actual.qaRevision());
        assertEquals(expected.verifiedByAccountId(), actual.verifiedByAccountId());
        assertEquals(expected.verifiedByMinecraftUUID(), actual.verifiedByMinecraftUUID());
        assertEquals(expected.verifiedEvidenceId(), actual.verifiedEvidenceId());
        assertEquals(expected.humanVerifiedAt(), actual.humanVerifiedAt());
        assertEquals(expected.humanVerified(), actual.humanVerified());
    }
}
