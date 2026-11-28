package org.tavall.qa.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardDesignCandidate;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.TaskBoardActionType;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardActionCreateRequest;
import org.tavall.qa.request.TaskBoardAssignmentCreateRequest;
import org.tavall.qa.request.TaskBoardDesignReviewAiReviewRequest;
import org.tavall.qa.request.TaskBoardDesignReviewDecisionRequest;
import org.tavall.qa.request.TaskBoardDesignReviewRegisterRequest;
import org.tavall.qa.request.TaskBoardEvidenceReviewRequest;
import org.tavall.qa.request.TaskBoardEvidenceSubmissionRequest;
import org.tavall.qa.request.TaskBoardHumanVerificationRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskAcceptanceRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real migrations, entity mapping, production composition and restart persistence. */
final class TaskBoardMutationHandlerSimulationTest {
    @TempDir Path directory;

    @Test
    void provisionalReviewSurvivesRestartAndHumanCanOverrideIt() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            UUID taskId;
            UUID actor = UUID.randomUUID();
            var owner = TaskBoardTestSubjects.manager(actor, UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var mutations = composition.bean(ITaskBoardMutationHandler.class);
                mutations.createProject(owner, new TaskBoardProjectCreateRequest("design-review", "Design review",
                        "Persisted review acceptance", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0));
                var task = mutations.createTask(owner, new TaskBoardTaskCreateRequest("design-review", "dashboard",
                        "Dashboard look", "Dashboard look", "Choose a live candidate", "Choose a live candidate",
                        TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, false, "test", "issue://23", 0));
                taskId = task.taskId();
                var review = mutations.registerDesignReview(owner, new TaskBoardDesignReviewRegisterRequest(taskId,
                        List.of(new TaskBoardDesignCandidate("A", "Ledger", "https://dashboard.tavall.org/__preview/design-review?direction=a", "Editorial"),
                                new TaskBoardDesignCandidate("B", "Console", "https://dashboard.tavall.org/__preview/design-review?direction=b", "Operational")), 0));
                assertFalse(review.deployable());
                var request = new TaskBoardDesignReviewAiReviewRequest(taskId, "A", "Better hierarchy",
                        List.of("evidence/1"), List.of("artifact/1"), "screenshots/1",
                        "gemini-4-neon-high", "design-reviewer", review.revision());
                var unprivileged = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(),
                        java.util.Set.of(), Optional.empty(), java.util.Set.of(), java.util.Set.of(),
                        List.of(), false, false);
                assertThrows(SecurityException.class, () -> mutations.recordDesignAiReview(unprivileged, request));
                var provisional = mutations.recordDesignAiReview(owner, request);
                assertEquals(2, provisional.revision());
                assertThrows(org.tavall.qa.domain.TaskBoardConcurrentModificationException.class,
                        () -> mutations.recordDesignAiReview(owner, request));
            }
            postgres.restart();
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var tasks = composition.bean(ITaskBoardDataHandler.class);
                var provisional = tasks.findDesignReview(taskId).orElseThrow();
                assertFalse(provisional.decided());
                assertNull(provisional.winnerKey());
                assertTrue(provisional.aiReviewed());
                assertTrue(provisional.deployable());
                assertEquals("A", provisional.aiCandidateKey());
                assertEquals("gemini-4-neon-high", provisional.aiReviewerModel());
                assertEquals(actor, provisional.aiReviewerAccountId());
                assertEquals("design-reviewer", provisional.aiReviewerAgentIdentity());
                assertNotNull(provisional.aiReviewedAt());
                assertEquals("Better hierarchy", provisional.aiRationale());
                assertEquals(List.of("evidence/1"), provisional.aiEvidenceRefs());
                assertEquals(List.of("artifact/1"), provisional.aiArtifactRefs());
                assertEquals("screenshots/1", provisional.aiScreenshotArtifactRef());
                assertEquals(TaskBoardTaskStatus.REVIEW, tasks.findTask(taskId).orElseThrow().status());
                var review = composition.bean(ITaskBoardMutationHandler.class).selectDesignReviewWinner(owner,
                        new TaskBoardDesignReviewDecisionRequest(taskId, "B", "Selected in review", provisional.revision()));
                assertTrue(review.decided());
                assertTrue(review.deployable());
                assertEquals("A", review.aiCandidateKey());
                assertEquals("B", review.winnerKey());
                assertEquals(actor, review.decidedByAccountId());
                assertNotNull(review.decidedAt());
                assertEquals("Selected in review", review.note());
                assertEquals(3, review.revision());
                assertEquals(TaskBoardTaskStatus.READY, tasks.findTask(taskId).orElseThrow().status());
                assertThrows(IllegalStateException.class, () -> composition.bean(ITaskBoardMutationHandler.class)
                        .selectDesignReviewWinner(owner, new TaskBoardDesignReviewDecisionRequest(taskId, "A", "", review.revision())));
                assertEquals("B", tasks.findDesignReview(taskId).orElseThrow().winnerKey());
            }
        }
    }

    @Test
    void dependencyMapCompositionSwapsManagedGenerationsWithoutReconstructingHandler() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            var owner = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var dependencies = composition.dependencies();
                var handler = composition.bean(ITaskBoardMutationHandler.class);

                // The composition boundary registers the handler under its interface and concrete tokens
                // plus the generated access surface, all sharing one metadata owner.
                assertSame(handler, dependencies.findInstance(ITaskBoardMutationHandler.class));
                assertSame(handler, composition.bean(TaskBoardMutationHandler.class));
                assertNotNull(dependencies.findInstance(TaskBoardMutationHandlerDependencyAccess.class));
                createProject(handler, owner);
                TaskBoardTask task = handler.createTask(owner, taskRequest("Di wiring swap task"));
                UUID taskId = task.taskId();

                // Dynamic generation swap: the same handler instance observes the replacement through
                // typed DependencyAccess without being reconstructed.
                ITaskBoardDataHandler realData = dependencies.findInstance(ITaskBoardDataHandler.class);
                ITaskBoardDataHandler swappedData = recordingDataHandler();
                dependencies.replaceInstance(ITaskBoardDataHandler.class, () -> swappedData);
                assertSame(handler, composition.bean(ITaskBoardMutationHandler.class));
                var swappedRequest = taskRequest("Must not resolve through the swapped generation");
                assertThrows(IllegalArgumentException.class, () -> handler.createTask(owner, swappedRequest),
                        "The swapped generation has no projects, so the same handler must fail over to it");
                dependencies.replaceInstance(ITaskBoardDataHandler.class, () -> realData);
                TaskBoardTask restored = handler.createTask(owner, taskRequest("Restored generation task"));
                assertSame(handler, composition.bean(ITaskBoardMutationHandler.class));
                assertNotNull(dependencies.findInstance(ITaskBoardDataHandler.class).findTask(taskId).orElseThrow(),
                        "The restored generation must still own the originally created task");

                // The action target resolver swaps the same way.
                ITaskBoardActionTargetResolver realResolver =
                        dependencies.findInstance(ITaskBoardActionTargetResolver.class);
                dependencies.replaceInstance(ITaskBoardActionTargetResolver.class,
                        () -> (ITaskBoardActionTargetResolver) Proxy.newProxyInstance(
                                ITaskBoardActionTargetResolver.class.getClassLoader(),
                                new Class<?>[]{ITaskBoardActionTargetResolver.class},
                                (proxy, method, args) -> {
                                    if (method.getName().equals("normalizeTarget")) {
                                        throw new IllegalArgumentException("Swapped resolver rejected the target");
                                    }
                                    return null;
                                }));
                assertThrows(IllegalArgumentException.class, () -> handler.addAction(owner, new TaskBoardActionCreateRequest(
                        taskId, TaskBoardActionType.EXTERNAL_LINK, "Issue", "https://tavall.org", 0)),
                        "The same handler must resolve action targets through the swapped generation");
                dependencies.replaceInstance(ITaskBoardActionTargetResolver.class, () -> realResolver);
                TaskBoardAction action = handler.addAction(owner, new TaskBoardActionCreateRequest(
                        taskId, TaskBoardActionType.EXTERNAL_LINK, "Issue", "https://tavall.org", 0));
                assertEquals("https://tavall.org", action.target());

                // Composition teardown retires the generation-owned handler fail-closed.
                composition.close();
                assertThrows(IllegalStateException.class, () -> handler.createTask(owner, taskRequest("After teardown")));
            }
        }
    }

    @Test
    void swappedDataGenerationVersionConflictsRollbackWithoutLostUpdate() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            var owner = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var dependencies = composition.dependencies();
                var handler = composition.bean(ITaskBoardMutationHandler.class);
                createProject(handler, owner);
                TaskBoardTask task = handler.createTask(owner, taskRequest("Version conflict target"));
                UUID taskId = task.taskId();
                ITaskBoardDataHandler realData = dependencies.findInstance(ITaskBoardDataHandler.class);

                // A replacement generation that always reports a stale expected version forces the
                // production conflict path through the same live handler instance.
                ITaskBoardDataHandler staleData = (ITaskBoardDataHandler) Proxy.newProxyInstance(
                        ITaskBoardDataHandler.class.getClassLoader(),
                        new Class<?>[]{ITaskBoardDataHandler.class},
                        (proxy, method, args) -> switch (method.getName()) {
                            case "saveTask" -> realData.saveTask((TaskBoardTask) args[0],
                                    Optional.of(((TaskBoardTask) args[0]).updatedAt().plusSeconds(1)),
                                    (org.tavall.qa.domain.TaskBoardAuditEntry) args[2]);
                            case "isAvailable" -> true;
                            case "toString" -> "swapped-stale-data-handler";
                            default -> invokeReal(realData, method, args);
                        });
                dependencies.replaceInstance(ITaskBoardDataHandler.class, () -> staleData);
                assertThrows(TaskBoardConcurrentModificationException.class, () -> handler.updateTask(owner,
                        updateTitle(taskId, "Must never be applied")));
                assertEquals(task.title(), dependencies.findInstance(ITaskBoardDataHandler.class)
                        .findTask(taskId).orElseThrow().title(), "The rejected mutation must roll back");
                assertEquals(task.qaRevision(), dependencies.findInstance(ITaskBoardDataHandler.class)
                        .findTask(taskId).orElseThrow().qaRevision());
                assertEquals(0, dependencies.findInstance(ITaskBoardDataHandler.class).findAuditEntries(taskId).stream()
                        .filter(entry -> entry.eventType() == TaskBoardAuditEventType.TASK_UPDATED).count());

                // Swapping the live generation back restores normal mutation on the same instance.
                dependencies.replaceInstance(ITaskBoardDataHandler.class, () -> realData);
                TaskBoardTask updated = handler.updateTask(owner, updateTitle(taskId, "Applied after swap back"));
                assertEquals("Applied after swap back", updated.title());
                assertEquals(task.qaRevision() + 1, updated.qaRevision());
                composition.close();
            }
        }
    }

    @Test
    void concurrentTaskUpdatesSerializePerVersionWithoutLostUpdatesOrAudit() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            var owner = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var handler = composition.bean(ITaskBoardMutationHandler.class);
                createProject(handler, owner);
                TaskBoardTask task = handler.createTask(owner, taskRequest("Concurrent update target"));
                UUID taskId = task.taskId();
                int workers = 8;
                Map<Integer, TaskBoardConcurrentModificationException> conflicts = new ConcurrentHashMap<>();
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch ready = new CountDownLatch(workers);
                try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
                    var futures = new ArrayList<java.util.concurrent.Future<TaskBoardTask>>();
                    for (int index = 0; index < workers; index++) {
                        int workerIndex = index;
                        futures.add(executor.submit(() -> {
                            ready.countDown();
                            assertTrue(start.await(10, TimeUnit.SECONDS), "Worker start timed out");
                            TaskBoardConcurrentModificationException lastConflict = null;
                            for (int attempt = 0; attempt < 200; attempt++) {
                                try {
                                    return handler.updateTask(owner, updateTitle(taskId,
                                            "Concurrent title " + workerIndex + " attempt " + attempt));
                                } catch (TaskBoardConcurrentModificationException conflict) {
                                    lastConflict = conflict;
                                }
                            }
                            conflicts.put(workerIndex, lastConflict);
                            throw new IllegalStateException("Worker " + workerIndex + " never committed");
                        }));
                    }
                    assertTrue(ready.await(10, TimeUnit.SECONDS));
                    start.countDown();
                    for (var future : futures) {
                        future.get(60, TimeUnit.SECONDS);
                    }
                }
                var persisted = composition.bean(ITaskBoardDataHandler.class);
                TaskBoardTask finalTask = persisted.findTask(taskId).orElseThrow();
                assertEquals(workers, finalTask.qaRevision(), "Every committed worker must advance the version");
                assertTrue(finalTask.title().startsWith("Concurrent title "),
                        "The surviving update must be one committed worker title, not a lost intermediate");
                assertEquals(workers, persisted.findAuditEntries(taskId).stream()
                        .filter(entry -> entry.eventType() == TaskBoardAuditEventType.TASK_UPDATED).count(),
                        "Conflicts must never persist audit entries; every commit must persist exactly one");
            }
        }
    }

    @Test
    void authorizationDenialsLeaveTaskBoardStateUnchanged() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            var manager = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var handler = composition.bean(ITaskBoardMutationHandler.class);
                var dataHandler = composition.bean(ITaskBoardDataHandler.class);
                createProject(handler, manager);
                TaskBoardTask task = handler.createTask(manager, taskRequest("Denial target"));
                UUID taskId = task.taskId();
                TaskBoardTask beforeDenials = dataHandler.findTask(taskId).orElseThrow();

                QaSubjectContext anonymous = QaSubjectContext.anonymous();
                QaSubjectContext viewer = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(),
                        Set.of(), Optional.empty(), Set.of(QaEnvironmentGrant.DEV),
                        Set.of(QaCapability.VIEW_QA_TASKS), List.of(), false, false);
                QaSubjectContext unassignedSubmitter = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(),
                        Set.of(), Optional.empty(), Set.of(QaEnvironmentGrant.DEV),
                        Set.of(QaCapability.SUBMIT_EVIDENCE), List.of(), false, false);

                assertThrows(SecurityException.class, () -> handler.createProject(anonymous, projectRequest()));
                assertThrows(SecurityException.class, () -> handler.createTask(anonymous, taskRequest("Anonymous task")));
                assertThrows(SecurityException.class, () -> handler.createTask(viewer, taskRequest("Viewer task")));
                assertThrows(SecurityException.class, () -> handler.updateTask(viewer, updateTitle(taskId, "Viewer update")));
                assertThrows(SecurityException.class, () -> handler.verifyTask(anonymous,
                        new TaskBoardHumanVerificationRequest(taskId, UUID.randomUUID(), QaEnvironmentGrant.DEV)));
                assertThrows(SecurityException.class, () -> handler.submitEvidence(unassignedSubmitter,
                        evidenceRequest(taskId)));
                assertThrows(SecurityException.class, () -> handler.addAction(viewer, new TaskBoardActionCreateRequest(
                        taskId, TaskBoardActionType.EXTERNAL_LINK, "Issue", "https://tavall.org", 0)));

                TaskBoardTask afterDenials = dataHandler.findTask(taskId).orElseThrow();
                assertEquals(beforeDenials.title(), afterDenials.title());
                assertEquals(beforeDenials.updatedAt(), afterDenials.updatedAt());
                assertEquals(beforeDenials.qaRevision(), afterDenials.qaRevision());
                assertEquals(0, dataHandler.findEvidenceForTask(taskId).size());
                assertEquals(0, dataHandler.findAuditEntries(taskId).stream()
                        .filter(entry -> entry.eventType() != TaskBoardAuditEventType.TASK_CREATED).count(),
                        "Denied operations must not record audit entries or mutate the task");
                composition.close();
            }
        }
    }

    @Test
    void rejectedEvidenceHumanVerifyReplayAndMaterialUpdateRollback() throws Exception {
        try (var postgres = new TaskBoardPostgresFixture(directory)) {
            var manager = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
            try (var composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC())) {
                var handler = composition.bean(ITaskBoardMutationHandler.class);
                var dataHandler = composition.bean(ITaskBoardDataHandler.class);
                createProject(handler, manager);
                TaskBoardTask task = handler.createTask(manager, taskRequest("Human verify flow target", true));
                UUID taskId = task.taskId();
                UUID contributorId = UUID.randomUUID();
                UUID verifierId = UUID.randomUUID();
                UUID verifierMinecraft = UUID.randomUUID();
                handler.assignTask(manager, new TaskBoardAssignmentCreateRequest(taskId, contributorId,
                        QaCapability.SUBMIT_EVIDENCE, QaEnvironmentGrant.DEV, Optional.empty(), Optional.empty(),
                        "QA simulation contributor"));
                handler.assignTask(manager, new TaskBoardAssignmentCreateRequest(taskId, verifierId,
                        QaCapability.HUMAN_VERIFY, QaEnvironmentGrant.DEV, Optional.empty(), Optional.empty(),
                        "QA simulation verifier"));
                var contributor = TaskBoardTestSubjects.assigned(contributorId, UUID.randomUUID(),
                        QaCapability.SUBMIT_EVIDENCE, taskId, QaEnvironmentGrant.DEV);
                var verifier = TaskBoardTestSubjects.assigned(verifierId, verifierMinecraft,
                        QaCapability.HUMAN_VERIFY, taskId, QaEnvironmentGrant.DEV);

                // Rejected evidence: the review decision persists, awards nothing, and can never verify.
                var rejectedSubmission = handler.submitEvidence(contributor, evidenceRequest(taskId));
                var rejected = handler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                        rejectedSubmission.evidenceId(), TaskBoardEvidenceStatus.REJECTED,
                        QaEnvironmentGrant.DEV, false, "Not reproducible"));
                assertEquals(TaskBoardEvidenceStatus.REJECTED, rejected.status());
                assertEquals(0, dataHandler.findVqcContributions(contributorId).size());
                assertEquals(TaskBoardQaStatus.NHV, dataHandler.findTask(taskId).orElseThrow().qaStatus());

                // Accepted evidence gates task acceptance, then Human Verify, with replay idempotence.
                var acceptedSubmission = handler.submitEvidence(contributor, evidenceRequest(taskId));
                handler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                        acceptedSubmission.evidenceId(), TaskBoardEvidenceStatus.ACCEPTED,
                        QaEnvironmentGrant.DEV, true, "Reproduced against the recorded build"));
                handler.updateTask(manager, new TaskBoardTaskUpdateRequest(taskId, null, null, null, null, null,
                        TaskBoardTaskStatus.REVIEW, null, null, null, null, null, null, null, null, null));
                TaskBoardTask accepted = handler.acceptTask(manager, new TaskBoardTaskAcceptanceRequest(
                        taskId, acceptedSubmission.evidenceId(), QaEnvironmentGrant.DEV));
                assertEquals(TaskBoardTaskStatus.DONE, accepted.status());
                assertEquals(TaskBoardQaStatus.NHV, accepted.qaStatus());
                assertEquals(1, dataHandler.findVqcContributions(contributorId).size());
                assertThrows(SecurityException.class, () -> handler.verifyTask(verifier,
                        new TaskBoardHumanVerificationRequest(taskId, rejectedSubmission.evidenceId(),
                                QaEnvironmentGrant.DEV)), "Rejected evidence must never Human Verify a DONE task");
                TaskBoardTask verified = handler.verifyTask(verifier,
                        new TaskBoardHumanVerificationRequest(taskId, acceptedSubmission.evidenceId(),
                                QaEnvironmentGrant.DEV));
                assertTrue(verified.humanVerified());
                assertEquals(verifierId, verified.verifiedByAccountId());
                assertEquals(verifierMinecraft, verified.verifiedByMinecraftUUID());
                assertEquals(acceptedSubmission.evidenceId(), verified.verifiedEvidenceId());
                TaskBoardTask replay = handler.verifyTask(verifier,
                        new TaskBoardHumanVerificationRequest(taskId, acceptedSubmission.evidenceId(),
                                QaEnvironmentGrant.DEV));
                assertTrue(replay.humanVerified());
                assertEquals(verified.qaStatus(), replay.qaStatus());
                assertEquals(verified.verifiedByAccountId(), replay.verifiedByAccountId());
                assertEquals(verified.verifiedByMinecraftUUID(), replay.verifiedByMinecraftUUID());
                assertEquals(verified.verifiedEvidenceId(), replay.verifiedEvidenceId());
                assertTrue(!replay.humanVerifiedAt().isBefore(verified.humanVerifiedAt()),
                        "Replaying verification keeps the task Human Verified with the same evidence");

                // A material update rolls the verify decision back and stales the prior evidence.
                TaskBoardTask rolledBack = handler.updateTask(manager, updateTitle(taskId, "Rolled back verify state"));
                assertEquals(TaskBoardQaStatus.REVIEW_REQUIRED, rolledBack.qaStatus());
                assertTrue(!rolledBack.humanVerified());
                assertTrue(rolledBack.verifiedByAccountId() == null);
                assertTrue(rolledBack.verifiedEvidenceId() == null);
                assertThrows(SecurityException.class, () -> handler.verifyTask(verifier,
                        new TaskBoardHumanVerificationRequest(taskId, acceptedSubmission.evidenceId(),
                                QaEnvironmentGrant.DEV)), "Stale evidence must not re-verify the rolled-back task");
                composition.close();
            }
        }
    }

    private void createProject(ITaskBoardMutationHandler handler, QaSubjectContext owner) {
        handler.createProject(owner, projectRequest());
    }

    private TaskBoardProjectCreateRequest projectRequest() {
        return new TaskBoardProjectCreateRequest("qa25-simulation", "QA 25 handler DI simulation",
                "Production-equivalent handler simulation project", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.STAFF, 0);
    }

    private TaskBoardTaskCreateRequest taskRequest(String title) {
        return taskRequest(title, false);
    }

    private TaskBoardTaskCreateRequest taskRequest(String title, boolean qaQcEligible) {
        return new TaskBoardTaskCreateRequest("qa25-simulation", "handler-di", title, title,
                "Task mutation handler DI reconciliation simulation", "Public simulation summary",
                TaskBoardTaskPriority.HIGH, TaskBoardAudience.STAFF, 1, false, qaQcEligible,
                "test", "issue://qa25", 0);
    }

    private TaskBoardTaskUpdateRequest updateTitle(UUID taskId, String title) {
        return new TaskBoardTaskUpdateRequest(taskId, null, title, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }

    private TaskBoardEvidenceSubmissionRequest evidenceRequest(UUID taskId) {
        return new TaskBoardEvidenceSubmissionRequest(taskId, QaEnvironmentGrant.DEV,
                "build-qa25", "Verify handler DI behavior", "The mutation applies once",
                "The mutation was observed once", "https://evidence.tavall.org/qa/qa25/receipt-1",
                "Simulated evidence for the handler DI reconciliation");
    }

    /** Records the swap visibility contract: a replacement generation the live handler must observe. */
    private ITaskBoardDataHandler recordingDataHandler() {
        return (ITaskBoardDataHandler) Proxy.newProxyInstance(
                ITaskBoardDataHandler.class.getClassLoader(),
                new Class<?>[]{ITaskBoardDataHandler.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "isAvailable" -> true;
                    case "findProject", "findTask", "findDesignReview", "findAssignment", "findEvidenceById" ->
                            Optional.empty();
                    case "findProjects", "findTasks", "findActions", "findComments", "findAssignments",
                            "findAssignmentsForAccount", "findEvidenceForTask", "findAuditEntries",
                            "findVqcContributions" -> List.of();
                    case "toString" -> "qa25-swapped-generation";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                });
    }

    private static Object invokeReal(ITaskBoardDataHandler target, java.lang.reflect.Method method, Object[] args)
            throws Exception {
        try {
            return method.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException invocationFailure) {
            Throwable cause = invocationFailure.getCause();
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Swapped generation call failed", cause);
        }
    }
}
