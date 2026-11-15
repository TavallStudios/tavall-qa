package org.tavall.qa.web.pages.venture;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct house-style test for {@link QaVenturesPage}: the real index page and its empty/unavailable states. */
final class QaVenturesPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void indexListsVenturesWithStageProgressAndOpenWork() {
        String html = renderer.render(QaVenturesPage.build(List.of(venture()), true)).body();

        assertTrue(html.contains("Ventures"));
        assertTrue(html.contains("href=\"/ventures\" aria-current=\"page\""));
        assertTrue(html.contains("Tavall Times"));
        assertTrue(html.contains("Active"));
        assertTrue(html.contains("2 open tasks"));
        assertTrue(html.contains("href=\"/ventures/tavall-times\""));
        assertTrue(html.contains("No live link yet"));
    }

    @Test
    void indexStatesUnavailableAndEmptyBoardsPlainlyInsteadOfInventingVentures() {
        String unavailable = renderer.render(QaVenturesPage.build(List.of(), false)).body();
        String empty = renderer.render(QaVenturesPage.build(List.of(), true)).body();

        assertTrue(unavailable.contains("This is not an empty portfolio."));
        assertTrue(unavailable.contains("Board unavailable"));
        assertTrue(empty.contains("No ventures yet"));
    }

    @Test
    void stageLabelHumanizesBoardStatusAndFallsBackWhenUnrecorded() {
        assertEquals("Active", QaVenturesPage.stageLabel("ACTIVE"));
        assertEquals("In review", QaVenturesPage.stageLabel("IN_REVIEW"));
        assertEquals("Stage not recorded", QaVenturesPage.stageLabel(""));
    }

    private static QaVentureView venture() {
        TaskBoardPublicProjectView project = new TaskBoardPublicProjectView(
                new TaskBoardPublicProjectSummaryView("tavall-times", "Tavall Times",
                        "Technology, gaming and culture publication.", TaskBoardProjectStatus.ACTIVE,
                        33, 3, 1, 1, 2, 1),
                List.of(new TaskBoardPublicSectionView("launch", 33, 1, 3, 3, 1, 1, 2, List.of(
                        task("Fix RSS", TaskBoardTaskStatus.READY, TaskBoardTaskPriority.NORMAL),
                        task("Old done task", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.HIGH),
                        task("Ship deals page", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.CRITICAL)))),
                List.of());
        return QaVentureView.fromProject(project, List.of(), List.of(),
                task -> "/ventures/tavall-times/tasks/" + task.taskId());
    }

    private static TaskBoardPublicTaskView task(String title, TaskBoardTaskStatus status, TaskBoardTaskPriority priority) {
        return new TaskBoardPublicTaskView(UUID.randomUUID(), "tavall-times", "launch", title, "", status, priority,
                TaskBoardQaStatus.NHV, 1, "github-issue", Instant.now(), List.of());
    }
}
