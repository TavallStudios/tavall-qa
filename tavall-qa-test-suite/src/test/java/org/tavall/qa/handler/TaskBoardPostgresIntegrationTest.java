package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardVqcContribution;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardAssignmentCreateRequest;
import org.tavall.qa.request.TaskBoardEvidenceReviewRequest;
import org.tavall.qa.request.TaskBoardEvidenceSubmissionRequest;
import org.tavall.qa.request.TaskBoardHumanVerificationRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskAcceptanceRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Production Task Board handlers and Tavall Database mappings against an isolated PostgreSQL database. */
@Execution(ExecutionMode.SAME_THREAD)
final class TaskBoardPostgresIntegrationTest {
    @TempDir static Path directory;
    private static TaskBoardPostgresFixture postgres;
    private TaskBoardPostgresComposition composition;

    @BeforeAll
    static void startIsolatedPostgres() throws Exception {
        postgres = new TaskBoardPostgresFixture(directory);
    }

    @AfterAll
    static void stopIsolatedPostgres() throws Exception {
        if (postgres != null) postgres.close();
    }

    @AfterEach
    void closeComposition() {
        if (composition != null) {
            composition.close();
            composition = null;
        }
    }

    @Test
    void acceptanceHumanVerificationRollbackConcurrencyAndRestartUseMappedJpaAuthority() throws Exception {
        composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC());
        ITaskBoardDataHandler data = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        UUID managerId = UUID.randomUUID();
        UUID managerMinecraft = UUID.randomUUID();
        QaSubjectContext manager = TaskBoardTestSubjects.manager(managerId, managerMinecraft);
        String projectKey = "pg-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        mutations.createProject(manager, new TaskBoardProjectCreateRequest(
                projectKey, "PostgreSQL Acceptance", "Isolated database fixture",
                org.tavall.qa.domain.TaskBoardProjectStatus.ACTIVE,
                org.tavall.qa.domain.TaskBoardAudience.PUBLIC, 0
        ));
        TaskBoardTask createdTask = mutations.createTask(manager, new TaskBoardTaskCreateRequest(
                projectKey, "acceptance", "Persisted QA acceptance", "Persisted QA acceptance",
                "Task requirements", "Public-safe requirements",
                org.tavall.qa.domain.TaskBoardTaskPriority.NORMAL,
                org.tavall.qa.domain.TaskBoardAudience.PUBLIC,
                3, true, true, "test", "https://example.com/source", 0
        ));
        TaskBoardTask task = mutations.updateTask(manager, new TaskBoardTaskUpdateRequest(
                createdTask.taskId(), null, null, null, null, null,
                TaskBoardTaskStatus.REVIEW, null, null, null, null, null, null, null, null, null
        ));

        UUID submitterId = UUID.randomUUID();
        UUID submitterMinecraft = UUID.randomUUID();
        mutations.assignTask(manager, new TaskBoardAssignmentCreateRequest(
                task.taskId(), submitterId, QaCapability.SUBMIT_EVIDENCE, QaEnvironmentGrant.DEV,
                Optional.of(QaClass.REGULAR), Optional.of(QaTier.TIER_I), "PostgreSQL integration fixture"
        ));
        QaSubjectContext submitter = TaskBoardTestSubjects.assigned(
                submitterId, submitterMinecraft, QaCapability.SUBMIT_EVIDENCE, task.taskId(), QaEnvironmentGrant.DEV
        );
        TaskBoardEvidence submittedEvidence = mutations.submitEvidence(submitter, new TaskBoardEvidenceSubmissionRequest(
                task.taskId(), QaEnvironmentGrant.DEV, "fixture-build", "task acceptance path",
                "Recorded behavior", "Observed behavior", "https://example.com/evidence", "Safe summary"
        ));
        TaskBoardEvidence evidence = mutations.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                submittedEvidence.evidenceId(), TaskBoardEvidenceStatus.ACCEPTED, QaEnvironmentGrant.DEV,
                true, "Accepted against the persisted task revision"
        ));
        assertEquals(0, data.findVqcContributions(submitterId).size(), "Evidence review alone must not award VQC");

        TaskBoardTask beforeAcceptance = data.findTask(task.taskId()).orElseThrow();
        TaskBoardTask invalidAccepted = new TaskBoardTask(
                beforeAcceptance.taskId(), beforeAcceptance.projectKey(), beforeAcceptance.sectionKey(),
                beforeAcceptance.title(), beforeAcceptance.publicTitle(), beforeAcceptance.description(),
                beforeAcceptance.publicSummary(), TaskBoardTaskStatus.DONE, beforeAcceptance.priority(),
                TaskBoardQaStatus.NHV, beforeAcceptance.audience(), beforeAcceptance.weight(),
                beforeAcceptance.hostValidationRequired(), beforeAcceptance.sourceType(),
                beforeAcceptance.sourceReference(), beforeAcceptance.qaQcEligible(),
                beforeAcceptance.qaRevision(), beforeAcceptance.createdByAccountId(),
                beforeAcceptance.createdByMinecraftUUID(), null, null, null, null,
                beforeAcceptance.sortOrder(), beforeAcceptance.createdAt(), Instant.now()
        );
        Instant invalidAcceptanceTime = Instant.now();
        TaskBoardAuditEntry invalidAudit = new TaskBoardAuditEntry(
                UUID.randomUUID(), Optional.of(task.taskId()), projectKey, Optional.of(managerId), managerMinecraft,
                TaskBoardAuditEventType.TASK_ACCEPTED, "Rollback fixture", invalidAcceptanceTime
        );
        TaskBoardVqcContribution invalidContribution = new TaskBoardVqcContribution(
                UUID.randomUUID(), task.taskId(), Optional.of(UUID.randomUUID()), Optional.of(submitterMinecraft),
                1, 0, false, "Intentionally invalid Account attribution", invalidAcceptanceTime
        );
        assertThrows(IllegalArgumentException.class, () -> data.acceptTask(
                invalidAccepted, Optional.of(beforeAcceptance.updatedAt()), evidence, invalidAudit,
                Optional.of(invalidContribution)
        ));
        assertEquals(beforeAcceptance, data.findTask(task.taskId()).orElseThrow(),
                "A failed acceptance must roll back the task update");
        assertFalse(data.findAuditEntries(task.taskId()).stream()
                        .anyMatch(event -> event.eventType() == TaskBoardAuditEventType.TASK_ACCEPTED),
                "A failed acceptance must roll back its audit entry");

        TaskBoardTaskAcceptanceRequest acceptance = new TaskBoardTaskAcceptanceRequest(
                task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV
        );
        CountDownLatch startAcceptance = new CountDownLatch(1);
        List<TaskBoardTask> acceptances;
        try (var workers = Executors.newFixedThreadPool(2)) {
            Future<TaskBoardTask> first = workers.submit(() -> acceptTaskWithOneRetry(
                    mutations, manager, acceptance, startAcceptance));
            Future<TaskBoardTask> second = workers.submit(() -> acceptTaskWithOneRetry(
                    mutations, manager, acceptance, startAcceptance));
            startAcceptance.countDown();
            acceptances = List.of(first.get(), second.get());
        }
        TaskBoardTask accepted = acceptances.getFirst();
        assertEquals(TaskBoardTaskStatus.DONE, accepted.status());
        assertEquals(TaskBoardQaStatus.NHV, accepted.qaStatus());
        assertEquals(1, data.findVqcContributions(submitterId).size());
        assertEquals(1, data.findAuditEntries(task.taskId()).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.TASK_ACCEPTED).count(),
                "Concurrent acceptance replay must be idempotent");

        UUID verifierId = UUID.randomUUID();
        UUID verifierMinecraft = UUID.randomUUID();
        mutations.assignTask(manager, new TaskBoardAssignmentCreateRequest(
                task.taskId(), verifierId, QaCapability.HUMAN_VERIFY, QaEnvironmentGrant.DEV,
                Optional.of(QaClass.REGULAR), Optional.of(QaTier.TIER_III), "Human Verify integration fixture"
        ));
        QaSubjectContext verifier = TaskBoardTestSubjects.assigned(
                verifierId, verifierMinecraft, QaCapability.HUMAN_VERIFY, task.taskId(), QaEnvironmentGrant.DEV
        );
        TaskBoardTask verified = mutations.verifyTask(verifier, new TaskBoardHumanVerificationRequest(
                task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV
        ));
        assertTrue(verified.humanVerified());
        assertEquals(verifierId, verified.verifiedByAccountId());
        assertEquals(evidence.evidenceId(), verified.verifiedEvidenceId());

        assertThrows(TaskBoardConcurrentModificationException.class, () -> data.saveTask(
                accepted, Optional.of(beforeAcceptance.updatedAt()), invalidAudit
        ));

        composition.close();
        composition = null;
        postgres.restart();
        composition = new TaskBoardPostgresComposition(postgres.database(), Clock.systemUTC());
        ITaskBoardDataHandler reopened = composition.bean(ITaskBoardDataHandler.class);
        TaskBoardTask recovered = reopened.findTask(task.taskId()).orElseThrow();
        assertTrue(recovered.humanVerified(), "Human Verify must survive database restart");
        assertEquals(verifierId, recovered.verifiedByAccountId());
        assertEquals(1, reopened.findVqcContributions(submitterId).size());
        assertTrue(reopened.findAuditEntries(task.taskId()).stream()
                .anyMatch(event -> event.eventType() == TaskBoardAuditEventType.TASK_ACCEPTED));
    }

    private static TaskBoardTask acceptTaskWithOneRetry(
            ITaskBoardMutationHandler mutations,
            QaSubjectContext manager,
            TaskBoardTaskAcceptanceRequest request,
            CountDownLatch start
    ) throws InterruptedException {
        start.await();
        try {
            return mutations.acceptTask(manager, request);
        } catch (TaskBoardConcurrentModificationException retryable) {
            return mutations.acceptTask(manager, request);
        }
    }
}
