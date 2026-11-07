package org.tavall.qa.web.pages.workspace;

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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaStaffWorkspacePage}: the real page composes the real
 * {@code QaStaffWorkspacePageHTML} builder with a real {@link TaskBoardTaskView}. No collaborators
 * are faked.
 */
final class QaStaffWorkspacePageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDirectionCModesAndPanels() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();

        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena",
                TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0, now, now
        );

        TaskBoardTask task = new TaskBoardTask(
                taskId,
                "tavall-pvp",
                "ffa",
                "Implement ranked queue",
                "Implement ranked queue",
                "Internal description",
                "Public summary",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.CRITICAL,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.PUBLIC,
                5,
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

        TaskBoardTaskView taskView = new TaskBoardTaskView(
                project, task, List.of(), List.of(), List.of(), List.of(), List.of()
        );

        QaWorkspacePageContext context = new QaWorkspacePageContext(
                "OVERVIEW", "CURRENT", "_csrf", "", Optional.empty(),
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), false, false, false
        );

        QaStaffWorkspacePage page = QaStaffWorkspacePage.build(taskView, context);
        String html = renderer.render(page).body();

        assertTrue(html.contains("Direction C · Focus Workspace"));
        assertTrue(html.contains("Evidence submissions"));
        assertTrue(html.contains("Recent task history"));
        assertTrue(html.contains("Human Verify"));
        assertTrue(html.contains("← Task detail"));
    }
}
