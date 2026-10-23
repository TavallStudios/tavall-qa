package org.tavall.qa.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.request.TaskBoardAssignmentCreateRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardEvidenceReviewRequest;
import org.tavall.qa.request.TaskBoardEvidenceSubmissionRequest;
import org.tavall.qa.request.TaskBoardHumanVerificationRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskAcceptanceRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardMutationHandlerTest {
    private final UUID managerId = UUID.randomUUID();
    private final UUID managerMinecraftUuid = UUID.randomUUID();
    private TaskBoardTestDataHandler dataHandler;
    private TaskBoardMutationHandler mutationHandler;
    private QaSubjectContext manager;

    @BeforeEach
    void setUp() {
        dataHandler = new TaskBoardTestDataHandler();
        mutationHandler = new TaskBoardMutationHandler(dataHandler, new TaskBoardActionTargetResolver());
        manager = TaskBoardTestSubjects.manager(managerId, managerMinecraftUuid);
        mutationHandler.createProject(manager, new TaskBoardProjectCreateRequest(
                "project-novus",
                "Project Novus",
                "Public development and QA",
                TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC,
                10
        ));
    }

    @Test
    void newTasksAlwaysEnterBacklogAndNhvAndRecordAccountCreator() {
        TaskBoardTask task = createTask(manager, false);

        assertEquals(TaskBoardTaskStatus.BACKLOG, task.status());
        assertEquals(TaskBoardQaStatus.NHV, task.qaStatus());
        assertFalse(task.humanVerified());
        assertEquals(managerId, task.createdByAccountId());
        assertEquals(managerMinecraftUuid, task.createdByMinecraftUUID());
        assertNull(task.verifiedByAccountId());
        assertNull(task.verifiedEvidenceId());
        assertNull(task.humanVerifiedAt());
    }

    @Test
    void doneDoesNotImplyHumanVerifiedAndPartialUpdatePreservesOtherFields() {
        TaskBoardTask task = createTask(manager, false);

        TaskBoardTask updated = mutationHandler.updateTask(manager, new TaskBoardTaskUpdateRequest(
                task.taskId(), null, null, null, null, null,
                TaskBoardTaskStatus.DONE, null, null, null, null, null, null, null, null, null
        ));

        assertEquals(TaskBoardTaskStatus.DONE, updated.status());
        assertEquals(TaskBoardQaStatus.NHV, updated.qaStatus());
        assertEquals(task.title(), updated.title());
        assertEquals(task.sourceReference(), updated.sourceReference());
        assertFalse(updated.humanVerified());
    }

    @Test
    void ordinaryAccountCannotMutateEvenWhenAuthenticated() {
        QaSubjectContext ordinary = TaskBoardTestSubjects.active(
                UUID.randomUUID(), null, java.util.Set.of(), Optional.empty(), java.util.Set.of(),
                java.util.Set.of(), java.util.List.of(), false, false
        );
        assertThrows(SecurityException.class, () -> mutationHandler.createTask(ordinary, createRequest(false)));
    }

    @Test
    void taskMustHaveAcceptedCurrentEvidenceAndLinkedMinecraftBeforeHumanVerify() {
        TaskBoardTask task = createTask(manager, true);
        TaskBoardTask done = mutationHandler.updateTask(manager, new TaskBoardTaskUpdateRequest(
                task.taskId(), null, null, null, null, null,
                TaskBoardTaskStatus.DONE, null, null, null, null, null, null, null, null, null
        ));
        UUID verifierId = UUID.randomUUID();
        UUID verifierMinecraft = UUID.randomUUID();
        assign(task.taskId(), verifierId, QaCapability.HUMAN_VERIFY);
        QaSubjectContext verifier = TaskBoardTestSubjects.assigned(
                verifierId, verifierMinecraft, QaCapability.HUMAN_VERIFY, task.taskId(), QaEnvironmentGrant.DEV
        );

        assertThrows(IllegalArgumentException.class, () -> mutationHandler.verifyTask(
                verifier,
                new TaskBoardHumanVerificationRequest(task.taskId(), UUID.randomUUID(), QaEnvironmentGrant.DEV)
        ));

        TaskBoardEvidenceSubmissionRequest submission = evidenceRequest(task.taskId());
        UUID submitterId = UUID.randomUUID();
        UUID submitterMinecraft = UUID.randomUUID();
        assign(task.taskId(), submitterId, QaCapability.SUBMIT_EVIDENCE);
        QaSubjectContext submitter = TaskBoardTestSubjects.assigned(
                submitterId, submitterMinecraft, QaCapability.SUBMIT_EVIDENCE, task.taskId(), QaEnvironmentGrant.DEV
        );
        var evidence = mutationHandler.submitEvidence(submitter, submission);
        assertEquals(TaskBoardEvidenceStatus.PENDING_REVIEW, evidence.status());
        var accepted = mutationHandler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                evidence.evidenceId(),
                TaskBoardEvidenceStatus.ACCEPTED,
                QaEnvironmentGrant.DEV,
                true,
                "Reproduced against the recorded build"
        ));
        assertTrue(accepted.publicSummaryApproved());
        assertEquals(0, dataHandler.findVqcContributions(submitterId).size(),
                "Evidence review alone must not award VQC");
        var duplicateEvidence = mutationHandler.submitEvidence(submitter, evidenceRequest(task.taskId()));
        mutationHandler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                duplicateEvidence.evidenceId(),
                TaskBoardEvidenceStatus.ACCEPTED,
                QaEnvironmentGrant.DEV,
                false,
                "Duplicate evidence for the same accepted task"
        ));
        assertEquals(0, dataHandler.findVqcContributions(submitterId).size(),
                "A second evidence review must not award VQC before task acceptance");

        TaskBoardTask acceptedTask = mutationHandler.acceptTask(manager, new TaskBoardTaskAcceptanceRequest(
                task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV
        ));
        assertEquals(TaskBoardTaskStatus.DONE, acceptedTask.status());
        assertEquals(TaskBoardQaStatus.NHV, acceptedTask.qaStatus());
        assertEquals(1, dataHandler.findVqcContributions(submitterId).size());
        assertTrue(dataHandler.findAuditEntries(task.taskId()).stream()
                .anyMatch(entry -> entry.eventType() == org.tavall.qa.domain.TaskBoardAuditEventType.TASK_ACCEPTED));

        TaskBoardTask verified = mutationHandler.verifyTask(
                verifier,
                new TaskBoardHumanVerificationRequest(task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV)
        );
        assertEquals(TaskBoardTaskStatus.DONE, verified.status());
        assertTrue(verified.humanVerified());
        assertEquals(verifierId, verified.verifiedByAccountId());
        assertEquals(verifierMinecraft, verified.verifiedByMinecraftUUID());
        assertEquals(evidence.evidenceId(), verified.verifiedEvidenceId());
        assertNotNull(verified.humanVerifiedAt());
        assertEquals(1, dataHandler.findVqcContributions(submitterId).size());

        TaskBoardTask invalidated = mutationHandler.updateTask(manager, new TaskBoardTaskUpdateRequest(
                task.taskId(), null, "Changed implementation requirements", null, null, null,
                null, null, null, null, null, null, null, null, null, null
        ));
        assertEquals(TaskBoardQaStatus.REVIEW_REQUIRED, invalidated.qaStatus());
        assertFalse(invalidated.humanVerified());
        assertNull(invalidated.verifiedByAccountId());
        assertNull(invalidated.verifiedByMinecraftUUID());
        assertNull(invalidated.verifiedEvidenceId());
        assertNull(invalidated.humanVerifiedAt());
        assertFalse(accepted.currentFor(org.tavall.qa.domain.TaskBoardTaskFingerprint.compute(invalidated)));
    }

    @Test
    void rejectedEvidenceDoesNotEarnVqcAndCannotBeHumanVerified() {
        TaskBoardTask task = createTask(manager, true);
        UUID contributorId = UUID.randomUUID();
        assign(task.taskId(), contributorId, QaCapability.SUBMIT_EVIDENCE);
        QaSubjectContext contributor = TaskBoardTestSubjects.assigned(
                contributorId, UUID.randomUUID(), QaCapability.SUBMIT_EVIDENCE,
                task.taskId(), QaEnvironmentGrant.DEV
        );
        var evidence = mutationHandler.submitEvidence(contributor, evidenceRequest(task.taskId()));
        var rejected = mutationHandler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                evidence.evidenceId(),
                TaskBoardEvidenceStatus.REJECTED,
                QaEnvironmentGrant.DEV,
                false,
                "The reported behavior could not be reproduced"
        ));

        assertEquals(TaskBoardEvidenceStatus.REJECTED, rejected.status());
        assertEquals(0, dataHandler.findVqcContributions(contributorId).size());
        assertTrue(dataHandler.findAuditEntries(task.taskId()).stream()
                .anyMatch(entry -> entry.eventType() == org.tavall.qa.domain.TaskBoardAuditEventType.EVIDENCE_REJECTED));
    }

    @Test
    void rejectingAcceptedTaskInvalidatesEvidenceAndPreventsRepeatVqc() {
        TaskBoardTask task = createTask(manager, true);
        mutationHandler.updateTask(manager, new TaskBoardTaskUpdateRequest(
                task.taskId(), null, null, null, null, null,
                TaskBoardTaskStatus.REVIEW, null, null, null, null, null, null, null, null, null
        ));
        UUID contributorId = UUID.randomUUID();
        assign(task.taskId(), contributorId, QaCapability.SUBMIT_EVIDENCE);
        QaSubjectContext contributor = TaskBoardTestSubjects.assigned(
                contributorId, UUID.randomUUID(), QaCapability.SUBMIT_EVIDENCE,
                task.taskId(), QaEnvironmentGrant.DEV
        );
        var evidence = mutationHandler.submitEvidence(contributor, evidenceRequest(task.taskId()));
        mutationHandler.reviewEvidence(manager, new TaskBoardEvidenceReviewRequest(
                evidence.evidenceId(), TaskBoardEvidenceStatus.ACCEPTED, QaEnvironmentGrant.DEV,
                false, "Evidence matches the current task version"
        ));
        mutationHandler.acceptTask(manager, new TaskBoardTaskAcceptanceRequest(
                task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV
        ));
        assertEquals(1, dataHandler.findVqcContributions(contributorId).size());

        mutationHandler.rejectTask(manager, task.taskId(), QaEnvironmentGrant.DEV, "Requirements need another pass");
        assertThrows(SecurityException.class, () -> mutationHandler.acceptTask(manager,
                new TaskBoardTaskAcceptanceRequest(task.taskId(), evidence.evidenceId(), QaEnvironmentGrant.DEV)));
        assertEquals(1, dataHandler.findVqcContributions(contributorId).size());
    }

    @Test
    void taskCannotBeRejectedBeforeItReachesReview() {
        TaskBoardTask task = createTask(manager, false);

        assertThrows(IllegalArgumentException.class, () -> mutationHandler.rejectTask(
                manager, task.taskId(), QaEnvironmentGrant.DEV, "No review result exists yet"
        ));
    }

    @Test
    void parentCommentMustBelongToSameTaskAndActorIsRecorded() {
        TaskBoardTask first = createTask(manager, false);
        TaskBoardTask second = createTask(manager, false);
        var firstComment = mutationHandler.addComment(manager, new TaskBoardCommentCreateRequest(
                first.taskId(), null, TaskBoardCommentType.COMMENT, "First task context", QaEnvironmentGrant.DEV
        ));

        assertEquals(managerId, firstComment.authorAccountId());
        assertThrows(IllegalArgumentException.class, () -> mutationHandler.addComment(
                manager,
                new TaskBoardCommentCreateRequest(
                        second.taskId(), firstComment.commentId(), TaskBoardCommentType.SUGGESTION,
                        "Wrong thread", QaEnvironmentGrant.DEV
                )
        ));
    }

    private void assign(UUID taskId, UUID assigneeId, QaCapability capability) {
        mutationHandler.assignTask(manager, new TaskBoardAssignmentCreateRequest(
                taskId,
                assigneeId,
                capability,
                QaEnvironmentGrant.DEV,
                Optional.of(QaClass.REGULAR),
                Optional.of(QaTier.TIER_I),
                "Issue 263 acceptance work"
        ));
    }

    private TaskBoardTask createTask(QaSubjectContext actor, boolean qaQcEligible) {
        return mutationHandler.createTask(actor, createRequest(qaQcEligible));
    }

    private TaskBoardTaskCreateRequest createRequest(boolean qaQcEligible) {
        return new TaskBoardTaskCreateRequest(
                "project-novus",
                "kingdom-systems",
                "Task " + UUID.randomUUID(),
                "Public kingdom task",
                "Task description",
                "Public-safe task summary",
                TaskBoardTaskPriority.HIGH,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                qaQcEligible,
                "github-pr",
                "TavallStudios/tavall-project-novus#263",
                0
        );
    }

    private TaskBoardEvidenceSubmissionRequest evidenceRequest(UUID taskId) {
        return new TaskBoardEvidenceSubmissionRequest(
                taskId,
                QaEnvironmentGrant.DEV,
                "a1234567890",
                "Verify completed task behavior",
                "The accepted task behavior is present",
                "The accepted task behavior was observed",
                "https://evidence.tavall.org/qa/263/receipt-1",
                "QA verified the task behavior in development"
        );
    }
}
