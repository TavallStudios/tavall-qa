package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.qa.auth.TaskBoardTestSubjects;
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
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardExternalReferenceDataHandler;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Owner-side reference acceptance through mapped JPA and the isolated native PostgreSQL fixture. */
@Execution(ExecutionMode.SAME_THREAD)
class TaskBoardExternalReferencePostgresIntegrationTest {
    @TempDir static Path directory;
    private static TaskBoardExecutionPostgresFixture postgres;

    @BeforeAll
    static void start() throws Exception {
        postgres = new TaskBoardExecutionPostgresFixture(directory);
    }

    @AfterAll
    static void stop() throws Exception {
        if (postgres != null) postgres.close();
    }

    @Test
    void concurrentSameIdentityAndExactReplayCreateOneReferenceAndAudit() throws Exception {
        var subjects = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        TaskBoardDataHandler tasks = new TaskBoardDataHandler(postgres.database());
        TaskBoardMutationHandler mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());
        ITaskBoardExternalReferenceHandler references = new TaskBoardExternalReferenceHandler(
                new TaskBoardExternalReferenceDataHandler(postgres.database()), tasks, Clock.systemUTC());
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
        TaskBoardDataHandler tasks = new TaskBoardDataHandler(postgres.database());
        TaskBoardMutationHandler mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());
        ITaskBoardExternalReferenceHandler references = new TaskBoardExternalReferenceHandler(
                new TaskBoardExternalReferenceDataHandler(postgres.database()), tasks, Clock.systemUTC());
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
        TaskBoardDataHandler tasks = new TaskBoardDataHandler(postgres.database());
        TaskBoardMutationHandler mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());
        ITaskBoardExternalReferenceHandler references = new TaskBoardExternalReferenceHandler(
                new TaskBoardExternalReferenceDataHandler(postgres.database()), tasks, Clock.systemUTC());
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
        TaskBoardDataHandler tasks = new TaskBoardDataHandler(postgres.database());
        TaskBoardMutationHandler mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());
        ITaskBoardExternalReferenceHandler references = new TaskBoardExternalReferenceHandler(
                new TaskBoardExternalReferenceDataHandler(postgres.database()), tasks, Clock.systemUTC());
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

    private static TaskBoardTask createTask(TaskBoardMutationHandler mutations,
            org.tavall.qa.domain.auth.QaSubjectContext subject, String title) {
        String projectKey = "extref-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        mutations.createProject(subject, new TaskBoardProjectCreateRequest(projectKey, "External reference tests",
                "Isolated database integration", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0));
        return mutations.createTask(subject, new TaskBoardTaskCreateRequest(projectKey, "reconciliation", title, title,
                "QA execution identity is separate from external source state", "External reference integration task",
                TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, false, "test", "source://" + projectKey, 0));
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
