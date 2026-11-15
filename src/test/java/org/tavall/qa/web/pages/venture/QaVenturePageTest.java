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
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct house-style test for {@link QaVenturePage}: the real detail page with recorded and missing venture data. */
final class QaVenturePageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void venturePageShowsPurposeStageLinksMetricsAndOpenTasks() {
        QaVentureView venture = venture(
                List.of(new ForgeLink("blog.tavall.org", "https://blog.tavall.org/")),
                List.of(new QaVentureView.Metric("Monthly readers", "1,200", "Analytics 2026-11")));

        String html = renderer.render(QaVenturePage.build(venture)).body();

        assertTrue(html.contains("Technology, gaming and culture publication."));
        assertTrue(html.contains("Active"));
        assertTrue(html.contains("href=\"https://blog.tavall.org/\""));
        assertTrue(html.contains("Monthly readers"));
        assertTrue(html.contains("1,200"));
        assertTrue(html.contains("Ship deals page"));
        assertFalse(html.contains("Old done task"));
        assertTrue(html.contains("href=\"/projects/tavall-times\""));
    }

    @Test
    void missingVentureDataIsSaidNotInvented() {
        String html = renderer.render(QaVenturePage.build(venture(List.of(), List.of()))).body();

        assertTrue(html.contains("No live link recorded yet."));
        assertTrue(html.contains("No metrics recorded yet."));
    }

    @Test
    void ventureWithNoOpenTasksSaysSoPlainly() {
        TaskBoardPublicProjectView project = new TaskBoardPublicProjectView(
                new TaskBoardPublicProjectSummaryView("tavall-times", "Tavall Times", "",
                        TaskBoardProjectStatus.ACTIVE, 0, 1, 0, 1, 0, 0),
                List.of(new TaskBoardPublicSectionView("launch", 0, 1, 1, 1, 0, 0, 0, List.of(
                        task("Ship deals page", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.CRITICAL)))),
                List.of());
        QaVentureView venture = QaVentureView.fromProject(project, List.of(), List.of(),
                task -> "/ventures/tavall-times/tasks/" + task.taskId());

        String html = renderer.render(QaVenturePage.build(venture)).body();

        assertTrue(html.contains("No open tasks."));
        assertTrue(html.contains("Purpose not recorded yet."));
        assertFalse(html.contains("Ship deals page"));
    }

    private static QaVentureView venture(List<ForgeLink> links, List<QaVentureView.Metric> metrics) {
        TaskBoardPublicProjectView project = new TaskBoardPublicProjectView(
                new TaskBoardPublicProjectSummaryView("tavall-times", "Tavall Times",
                        "Technology, gaming and culture publication.", TaskBoardProjectStatus.ACTIVE,
                        33, 3, 1, 1, 2, 1),
                List.of(new TaskBoardPublicSectionView("launch", 33, 1, 3, 3, 1, 1, 2, List.of(
                        task("Fix RSS", TaskBoardTaskStatus.READY, TaskBoardTaskPriority.NORMAL),
                        task("Old done task", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.HIGH),
                        task("Ship deals page", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.CRITICAL)))),
                List.of());
        return QaVentureView.fromProject(project, links, metrics,
                task -> "/ventures/tavall-times/tasks/" + task.taskId());
    }

    private static TaskBoardPublicTaskView task(String title, TaskBoardTaskStatus status, TaskBoardTaskPriority priority) {
        return new TaskBoardPublicTaskView(UUID.randomUUID(), "tavall-times", "launch", title, "", status, priority,
                TaskBoardQaStatus.NHV, 1, "github-issue", Instant.now(), List.of());
    }
}
