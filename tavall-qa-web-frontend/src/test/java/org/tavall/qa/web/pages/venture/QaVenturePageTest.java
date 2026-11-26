package org.tavall.qa.web.pages.venture;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectKey;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaSubjectStatus;
import org.tavall.qa.handler.ITaskBoardVentureQueryHandler;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.web.frontend.design.ForgeLink;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
        assertTrue(html.contains("href=\"https://qa.tavall.org/projects/tavall-times\""));
    }

    @Test
    void typedVenturePageShowsBlockerAndLinksTasksToEngineering() {
        TaskBoardVenture record = new TaskBoardVenture(
                "tavall-qa", "venture/tavall-qa/venture-card", "QA authority",
                List.of("tavall-qa", "tavall-web-qa"), "LIVE", List.of(),
                "Authenticated Web route adapter remains to be wired", null);
        QaVentureView view = QaVentureView.fromVenture(record);

        String html = renderer.render(QaVenturePage.build(view)).body();

        assertTrue(html.contains("QA authority"));
        assertTrue(html.contains("tavall-web-qa"));
        assertTrue(html.contains("Authenticated Web route adapter remains to be wired"));
        assertTrue(html.contains("href=\"https://qa.tavall.org/projects/tavall-engineering\""));
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

    @Test
    void handlerBackedBuildRendersResolvedVentureAndMatchedEngineeringTasks() {
        UUID taskId = UUID.randomUUID();
        TaskBoardVenture record = new TaskBoardVenture(
                "tavall-qa", "venture/tavall-qa/venture-card", "QA authority",
                List.of("tavall-qa"), "LIVE", List.of(), "Wiring", null);

        String html = renderer.render(QaVenturePage.build(
                new StubVentureQueryHandler(record, List.of(linkedTask(taskId))),
                "tavall-qa", ventureManager())).body();

        assertTrue(html.contains("QA authority"));
        assertTrue(html.contains("Add venture query"));
        assertTrue(html.contains("href=\"https://qa.tavall.org/projects/tavall-engineering/tasks/" + taskId + "\""));
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

    private static TaskBoardTask linkedTask(UUID taskId) {
        Instant created = Instant.parse("2026-11-20T09:00:00Z");
        return new TaskBoardTask(taskId, TaskBoardProjectKey.ENGINEERING.value(), "qa", "Add venture query",
                "Add venture query", "", "", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV, TaskBoardAudience.ADMIN_ONLY, 1, false, "github-issue",
                "https://github.com/TavallStudios/tavall-qa/issues/38", true, 1,
                UUID.randomUUID(), null, null, null, null, null, 1, created, created);
    }

    private static QaSubjectContext ventureManager() {
        return new QaSubjectContext(Optional.of(new UUID(0, 20)), QaSubjectStatus.ACTIVE,
                Optional.empty(), Optional.of(new UUID(0, 30)), true, false,
                Set.of(), Optional.empty(), Set.of(),
                Set.of(QaCapability.MANAGE_PROJECTS), List.of(), false);
    }

    private static final class StubVentureQueryHandler implements ITaskBoardVentureQueryHandler {
        private final TaskBoardVenture venture;
        private final List<TaskBoardTask> tasks;

        private StubVentureQueryHandler(TaskBoardVenture venture, List<TaskBoardTask> tasks) {
            this.venture = venture;
            this.tasks = List.copyOf(tasks);
        }

        @Override
        public List<TaskBoardVenture> findVentures(QaSubjectContext subject) {
            return List.of(venture);
        }

        @Override
        public Optional<TaskBoardVenture> findVenture(String slug, QaSubjectContext subject) {
            return venture.slug().equals(slug) ? Optional.of(venture) : Optional.empty();
        }

        @Override
        public List<TaskBoardTask> findVentureTasks(String slug, QaSubjectContext subject) {
            return tasks;
        }
    }
}
