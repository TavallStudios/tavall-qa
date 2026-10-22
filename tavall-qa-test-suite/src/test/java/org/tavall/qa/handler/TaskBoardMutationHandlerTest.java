package org.tavall.qa.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardMutationHandlerTest {
    private final UUID humanUUID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private TaskBoardTestDataHandler dataHandler;
    private TaskBoardMutationHandler mutationHandler;

    @BeforeEach
    void setUp() {
        dataHandler = new TaskBoardTestDataHandler();
        mutationHandler = new TaskBoardMutationHandler(
                dataHandler,
                new TaskBoardActionTargetResolver()
        );
        mutationHandler.createProject(new TaskBoardProjectCreateRequest(
                "project-novus",
                "Project Novus",
                "Public development and QA",
                TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC,
                10
        ));
    }

    @Test
    void newTasksAlwaysEnterBacklogAndNhv() {
        TaskBoardTask task = createTask(humanUUID);

        assertEquals(TaskBoardTaskStatus.BACKLOG, task.status());
        assertEquals(TaskBoardQaStatus.NHV, task.qaStatus());
        assertFalse(task.humanVerified());
        assertNull(task.verifiedByMinecraftUUID());
        assertNull(task.humanVerifiedAt());
    }

    @Test
    void taskCarriesPublicProgressMetadataWithoutProjectionForeignKey() {
        TaskBoardTask task = createTask(humanUUID);

        assertEquals("kingdom-systems", task.sectionKey());
        assertEquals(3, task.weight());
        assertEquals("github-pr", task.sourceType());
        assertEquals("TavallStudios/tavall-project-novus#263", task.sourceReference());
        assertEquals(TaskBoardAudience.PUBLIC, task.audience());
    }

    @Test
    void doneDoesNotImplyHumanVerified() {
        TaskBoardTask task = createTask(humanUUID);

        TaskBoardTask updated = mutationHandler.updateTask(updateRequest(
                task,
                TaskBoardTaskStatus.DONE,
                TaskBoardQaStatus.NHV,
                humanUUID
        ));

        assertEquals(TaskBoardTaskStatus.DONE, updated.status());
        assertEquals(TaskBoardQaStatus.NHV, updated.qaStatus());
        assertFalse(updated.humanVerified());
    }

    @Test
    void humanVerifiedRequiresLinkedHumanIdentity() {
        TaskBoardTask task = createTask(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mutationHandler.updateTask(updateRequest(
                        task,
                        TaskBoardTaskStatus.REVIEW,
                        TaskBoardQaStatus.HUMAN_VERIFIED,
                        null
                ))
        );

        assertTrue(exception.getMessage().contains("Only completed tasks can be Human Verified")
                || exception.getMessage().contains("linked Minecraft identity"));
    }

    @Test
    void humanVerifiedRecordsVerifierAndTimestamp() {
        TaskBoardTask task = createTask(humanUUID);

        TaskBoardTask verified = mutationHandler.updateTask(updateRequest(
                task,
                TaskBoardTaskStatus.DONE,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                humanUUID
        ));

        assertTrue(verified.humanVerified());
        assertEquals(humanUUID, verified.verifiedByMinecraftUUID());
        assertNotNull(verified.humanVerifiedAt());
    }

    @Test
    void removingHumanVerifiedClearsVerificationEvidence() {
        TaskBoardTask task = createTask(humanUUID);
        TaskBoardTask verified = mutationHandler.updateTask(updateRequest(
                task,
                TaskBoardTaskStatus.DONE,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                humanUUID
        ));

        TaskBoardTask reopened = mutationHandler.updateTask(updateRequest(
                verified,
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardQaStatus.REVIEW_REQUIRED,
                humanUUID
        ));

        assertEquals(TaskBoardQaStatus.REVIEW_REQUIRED, reopened.qaStatus());
        assertNull(reopened.verifiedByMinecraftUUID());
        assertNull(reopened.humanVerifiedAt());
    }

    @Test
    void materialChangeWithoutReverificationInvalidatesHumanVerification() {
        TaskBoardTask task = createTask(humanUUID);
        TaskBoardTask verified = mutationHandler.updateTask(updateRequest(
                task,
                TaskBoardTaskStatus.DONE,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                humanUUID
        ));
        assertTrue(verified.humanVerified());

        // Update task with changed title/requirements without supplying re-verifier actor
        TaskBoardTaskUpdateRequest changeReq = new TaskBoardTaskUpdateRequest(
                verified.taskId(),
                verified.sectionKey(),
                "Changed Material Requirements Title",
                verified.publicTitle(),
                verified.description(),
                verified.publicSummary(),
                TaskBoardTaskStatus.DONE,
                verified.priority(),
                TaskBoardQaStatus.HUMAN_VERIFIED,
                verified.audience(),
                verified.weight(),
                verified.hostValidationRequired(),
                verified.sourceType(),
                verified.sourceReference(),
                null, // no re-verification identity provided
                verified.sortOrder()
        );

        TaskBoardTask invalidated = mutationHandler.updateTask(changeReq);
        assertEquals(TaskBoardQaStatus.REVIEW_REQUIRED, invalidated.qaStatus());
        assertFalse(invalidated.humanVerified());
        assertNull(invalidated.verifiedByMinecraftUUID());
    }

    @Test
    void parentCommentMustBelongToSameTask() {
        TaskBoardTask first = createTask(humanUUID);
        TaskBoardTask second = createTask(humanUUID);
        var firstComment = mutationHandler.addComment(new TaskBoardCommentCreateRequest(
                first.taskId(),
                null,
                humanUUID,
                TaskBoardCommentType.COMMENT,
                "First task context"
        ));

        assertThrows(IllegalArgumentException.class, () -> mutationHandler.addComment(
                new TaskBoardCommentCreateRequest(
                        second.taskId(),
                        firstComment.commentId(),
                        humanUUID,
                        TaskBoardCommentType.SUGGESTION,
                        "Wrong thread"
                )
        ));
    }

    private TaskBoardTask createTask(UUID actorMinecraftUUID) {
        return mutationHandler.createTask(new TaskBoardTaskCreateRequest(
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
                "github-pr",
                "TavallStudios/tavall-project-novus#263",
                actorMinecraftUUID,
                0
        ));
    }

    private TaskBoardTaskUpdateRequest updateRequest(
            TaskBoardTask task,
            TaskBoardTaskStatus status,
            TaskBoardQaStatus qaStatus,
            UUID actorMinecraftUUID
    ) {
        return new TaskBoardTaskUpdateRequest(
                task.taskId(),
                task.sectionKey(),
                task.title(),
                task.publicTitle(),
                task.description(),
                task.publicSummary(),
                status,
                task.priority(),
                qaStatus,
                task.audience(),
                task.weight(),
                task.hostValidationRequired(),
                task.sourceType(),
                task.sourceReference(),
                actorMinecraftUUID,
                task.sortOrder()
        );
    }
}
