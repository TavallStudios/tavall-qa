package org.tavall.qa.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TaskBoardTaskFingerprintTest {
    private static final UUID TASK_ID = UUID.fromString("c98c8b63-82f5-44fd-8cf1-066f890b60c1");
    private static final Instant CREATED_AT = Instant.parse("2026-10-31T12:00:00Z");

    @Test
    void computesAStableSha256FingerprintForTheSameTask() {
        TaskBoardTask task = task("Original title", "tavall-qa", TaskBoardTaskStatus.ACTIVE);

        String fingerprint = TaskBoardTaskFingerprint.compute(task);

        assertTrue(fingerprint.matches("[a-f0-9]{64}"));
        assertEquals(fingerprint, TaskBoardTaskFingerprint.compute(task));
    }

    @Test
    void aChangeToReviewedTaskContentChangesTheFingerprint() {
        String original = TaskBoardTaskFingerprint.compute(
                task("Original title", "tavall-qa", TaskBoardTaskStatus.ACTIVE));

        String renamed = TaskBoardTaskFingerprint.compute(
                task("Updated title", "tavall-qa", TaskBoardTaskStatus.ACTIVE));
        String movedToAnotherProject = TaskBoardTaskFingerprint.compute(
                task("Original title", "tavall-cloud", TaskBoardTaskStatus.ACTIVE));

        assertNotEquals(original, renamed);
        assertNotEquals(original, movedToAnotherProject);
    }

    @Test
    void workflowStatusDoesNotChangeTheReviewedContentFingerprint() {
        String active = TaskBoardTaskFingerprint.compute(
                task("Original title", "tavall-qa", TaskBoardTaskStatus.ACTIVE));
        String done = TaskBoardTaskFingerprint.compute(
                task("Original title", "tavall-qa", TaskBoardTaskStatus.DONE));

        assertEquals(active, done);
    }

    @Test
    void rejectsAMissingTask() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> TaskBoardTaskFingerprint.compute(null));

        assertEquals("task", exception.getMessage());
    }

    private static TaskBoardTask task(String title, String projectKey, TaskBoardTaskStatus status) {
        return new TaskBoardTask(
                TASK_ID,
                projectKey,
                "qa",
                title,
                "Public title",
                "Description",
                "Public summary",
                status,
                TaskBoardTaskPriority.NORMAL,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.QA_TESTER,
                3,
                true,
                "github-issue",
                "TavallStudios/tavall-qa#10",
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                4,
                CREATED_AT,
                CREATED_AT.plusSeconds(60)
        );
    }
}
