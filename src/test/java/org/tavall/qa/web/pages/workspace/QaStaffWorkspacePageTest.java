package org.tavall.qa.web.pages.workspace;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaStaffWorkspacePage}: the real page composes the real
 * {@code QaStaffWorkspacePageHTML} builder. No collaborators are faked.
 */
final class QaStaffWorkspacePageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDirectionCModesAndPanels() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardTaskPageView task = new TaskBoardTaskPageView(
                taskId,
                "tavall-pvp",
                "Tavall PvP",
                "ffa",
                "Implement ranked queue",
                "Summary",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.CRITICAL,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.PUBLIC,
                5,
                "github",
                now,
                true
        );

        QaStaffWorkspacePage page = QaStaffWorkspacePage.build(task, "QA", "CURRENT", true, "verifier-uuid");
        String html = renderer.render(page).body();

        assertTrue(html.contains("Direction C · Focus Workspace"));
        assertTrue(html.contains("Overview"));
        assertTrue(html.contains("Qa"));
        assertTrue(html.contains("Edit"));
        assertTrue(html.contains("Thread"));
        assertTrue(html.contains("Record QA Decision"));
    }
}
