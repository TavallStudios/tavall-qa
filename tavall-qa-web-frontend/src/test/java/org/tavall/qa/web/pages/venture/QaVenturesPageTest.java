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
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void handlerBackedBuildResolvesVenturesAndTheirEngineeringTasks() {
        UUID taskId = UUID.randomUUID();
        TaskBoardVenture venture = new TaskBoardVenture("tavall-qa", "venture/tavall-qa/venture-card",
                "QA authority", List.of("tavall-qa"), "LIVE", List.of(), "Wiring", null);

        String html = renderer.render(QaVenturesPage.build(
                new StubVentureQueryHandler(venture, List.of(linkedTask(taskId))), ventureManager())).body();

        assertTrue(html.contains("Tavall Qa"));
        assertTrue(html.contains("1 open task"));
        assertTrue(html.contains("href=\"/ventures/tavall-qa\""));
    }

    @Test
    void handlerBackedBuildShowsUnavailableWhenVentureStorageFails() {
        String html = renderer.render(QaVenturesPage.build(
                new UnavailableVentureQueryHandler(), ventureManager())).body();

        assertTrue(html.contains("Board unavailable"));
        assertTrue(html.contains("This is not an empty portfolio."));
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

    private static final class UnavailableVentureQueryHandler implements ITaskBoardVentureQueryHandler {
        @Override
        public List<TaskBoardVenture> findVentures(QaSubjectContext subject) {
            throw new org.tavall.qa.domain.TaskBoardStorageUnavailableException("unavailable");
        }

        @Override
        public Optional<TaskBoardVenture> findVenture(String slug, QaSubjectContext subject) {
            throw new org.tavall.qa.domain.TaskBoardStorageUnavailableException("unavailable");
        }

        @Override
        public List<TaskBoardTask> findVentureTasks(String slug, QaSubjectContext subject) {
            throw new org.tavall.qa.domain.TaskBoardStorageUnavailableException("unavailable");
        }
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
