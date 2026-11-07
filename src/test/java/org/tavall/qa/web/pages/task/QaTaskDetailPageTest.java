package org.tavall.qa.web.pages.task;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaTaskDetailPage}: the real page composes the real
 * {@code QaTaskDetailPageHTML} builder with real domain views. No collaborators are faked.
 */
final class QaTaskDetailPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDirectionAEvidenceLayoutAndNhvBadge() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0, now, now
        );

        TaskBoardTask taskNhv = new TaskBoardTask(
                taskId,
                "tavall-pvp",
                "ffa",
                "Fix combo knockback vector",
                "Fix combo knockback vector",
                "Internal description of vector mechanics",
                "Public summary of knockback",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263",
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                0,
                now,
                now
        );

        TaskBoardTaskView viewNhv = new TaskBoardTaskView(project, taskNhv, List.of(), List.of(), List.of(), List.of(), List.of());
        QaTaskDetailPage pageNhv = QaTaskDetailPage.build(viewNhv, false, false);
        String htmlNhv = renderer.render(pageNhv).body();

        assertTrue(htmlNhv.contains("Fix combo knockback vector"));
        assertTrue(htmlNhv.contains("NHV — Not Human Verified"));
        assertFalse(htmlNhv.contains("HUMAN VERIFIED"));

        UUID verifierUuid = UUID.randomUUID();
        TaskBoardTask taskVerified = new TaskBoardTask(
                taskId,
                "tavall-pvp",
                "ffa",
                "Fix combo knockback vector",
                "Fix combo knockback vector",
                "Internal description of vector mechanics",
                "Public summary of knockback",
                TaskBoardTaskStatus.DONE,
                TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263",
                true,
                UUID.randomUUID(),
                UUID.randomUUID(),
                verifierUuid,
                UUID.randomUUID(),
                UUID.randomUUID(),
                now,
                0,
                now,
                now
        );

        TaskBoardTaskView viewVerified = new TaskBoardTaskView(project, taskVerified, List.of(), List.of(), List.of(), List.of(), List.of());
        QaTaskDetailPage pageVerified = QaTaskDetailPage.build(viewVerified, true, true);
        String htmlVerified = renderer.render(pageVerified).body();

        assertTrue(htmlVerified.contains("HUMAN VERIFIED"));
        assertTrue(htmlVerified.contains(verifierUuid.toString()));
    }
}
