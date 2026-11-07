package org.tavall.qa.web.pages.project;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaProjectDetailPage}: the real page composes the real
 * {@code QaProjectDetailPageHTML} builder. No collaborators are faked.
 */
final class QaProjectDetailPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDirectionA() {
        UUID taskId1 = UUID.randomUUID();
        UUID taskId2 = UUID.randomUUID();
        UUID taskId3 = UUID.randomUUID();
        UUID taskId4 = UUID.randomUUID();

        TaskBoardPublicTaskView t1 = new TaskBoardPublicTaskView(taskId1, "tavall-pvp", "ffa", "Task 1", "Summary 1", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.HUMAN_VERIFIED, 1, "github", Instant.now(), List.of());
        TaskBoardPublicTaskView t2 = new TaskBoardPublicTaskView(taskId2, "tavall-pvp", "ffa", "Task 2", "Summary 2", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH, TaskBoardQaStatus.NHV, 2, "github", Instant.now(), List.of());
        TaskBoardPublicTaskView t3 = new TaskBoardPublicTaskView(taskId3, "tavall-pvp", "ffa", "Task 3", "Summary 3", TaskBoardTaskStatus.BACKLOG, TaskBoardTaskPriority.LOW, TaskBoardQaStatus.NHV, 1, "github", Instant.now(), List.of());
        TaskBoardPublicTaskView t4 = new TaskBoardPublicTaskView(taskId4, "tavall-pvp", "ffa", "Task 4", "Summary 4", TaskBoardTaskStatus.BACKLOG, TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV, 1, "github", Instant.now(), List.of());

        TaskBoardPublicSectionView section = new TaskBoardPublicSectionView("ffa", 50, 2, 4, 4, 1, 1, 3, List.of(t1, t2, t3, t4));

        TaskBoardPublicProjectSummaryView summary = new TaskBoardPublicProjectSummaryView(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE, 50, 4, 1, 1, 3, 1
        );

        TaskBoardPublicProjectView projectView = new TaskBoardPublicProjectView(summary, List.of(section), List.of());

        QaProjectDetailPage page = QaProjectDetailPage.build(projectView, true, true, 3, 0);
        String html = renderer.render(page).body();

        assertTrue(html.contains("Tavall PvP"));
        assertTrue(html.contains("Show more tasks"));
        assertTrue(html.contains("Visible tasks"));
        assertTrue(html.contains("Task 1"));
        assertTrue(html.contains("Task 4"));
    }
}
