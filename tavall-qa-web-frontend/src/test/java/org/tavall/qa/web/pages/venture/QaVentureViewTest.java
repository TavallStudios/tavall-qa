package org.tavall.qa.web.pages.venture;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Direct house-style test for {@link QaVentureView}: board projection with real domain values. */
final class QaVentureViewTest {

    @Test
    void fromProjectExcludesClosedWorkAndMostUrgentComesFirst() {
        QaVentureView venture = QaVentureView.fromProject(project(), List.of(), List.of(),
                task -> "/tasks/" + task.taskId());

        assertEquals(List.of("Ship deals page", "Fix RSS"),
                venture.openTasks().stream().map(QaVentureView.OpenTask::title).toList(),
                "done work is excluded and critical work comes before normal work");
        assertEquals(2, venture.openTaskCount());
        assertEquals(3, venture.taskCount());
        assertEquals(33, venture.progressPercent());
    }

    @Test
    void fromProjectUsesRecordedProjectFactsAndBuildsTaskHrefThroughTheSuppliedMapper() {
        QaVentureView venture = QaVentureView.fromProject(project(), List.of(), List.of(),
                task -> "/projects/tavall-times/tasks/" + task.taskId());

        assertEquals("tavall-times", venture.key());
        assertEquals("Tavall Times", venture.name());
        assertEquals("Technology, gaming and culture publication.", venture.purpose());
        assertEquals("ACTIVE", venture.stage());
        assertTrue(venture.openTasks().stream()
                .allMatch(task -> task.href().startsWith("/projects/tavall-times/tasks/")));
        assertEquals(TaskBoardTaskStatus.ACTIVE, venture.openTasks().getFirst().status());
    }

    @Test
    void fromVentureLinksFilteredWorkBackToTheEngineeringProject() {
        UUID taskId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-20T09:00:00Z");
        TaskBoardVenture ventureData = new TaskBoardVenture(
                "tavall-qa", "venture/tavall-qa/venture-card", "QA authority",
                List.of("tavall-qa", "tavall-web-qa"), "LIVE", List.of(), "API consumer wiring", null);
        TaskBoardTask linkedTask = new TaskBoardTask(taskId, "tavall-engineering", "qa", "Add venture query",
                "Add venture query", "", "", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV, TaskBoardAudience.ADMIN_ONLY, 1, false, "github-issue",
                "https://github.com/TavallStudios/tavall-qa/issues/38", true, 1,
                UUID.randomUUID(), null, null, null, null, null, 1, createdAt, createdAt);

        QaVentureView venture = QaVentureView.fromVenture(ventureData, List.of(linkedTask));

        assertEquals("tavall-qa", venture.key());
        assertEquals(List.of("tavall-qa", "tavall-web-qa"), venture.ownerRepos());
        assertEquals("API consumer wiring", venture.topBlocker());
        assertEquals("https://qa.tavall.org/projects/tavall-engineering/tasks/" + taskId,
                venture.openTasks().getFirst().href());
        assertEquals(-1, venture.progressPercent());
    }

    @Test
    void unrecordedFactsStayEmptyAndKeysAreTrimmed() {
        QaVentureView venture = new QaVentureView("  tavall-times  ", "  Tavall Times  ",
                null, null, List.of(), List.of(), 0, 0, List.of());

        assertEquals("tavall-times", venture.key());
        assertEquals("Tavall Times", venture.name());
        assertEquals("", venture.purpose());
        assertEquals("", venture.stage());
        assertEquals(0, venture.openTaskCount());
    }

    @Test
    void nullKeyIsRejectedAtTheBoundary() {
        assertThrows(NullPointerException.class, () -> new QaVentureView(null, "Tavall Times",
                "", "", List.of(), List.of(), 0, 0, List.of()));
    }

    private static TaskBoardPublicProjectView project() {
        return new TaskBoardPublicProjectView(
                new TaskBoardPublicProjectSummaryView("tavall-times", "Tavall Times",
                        "Technology, gaming and culture publication.", TaskBoardProjectStatus.ACTIVE,
                        33, 3, 1, 1, 2, 1),
                List.of(new TaskBoardPublicSectionView("launch", 33, 1, 3, 3, 1, 1, 2, List.of(
                        task("Fix RSS", TaskBoardTaskStatus.READY, TaskBoardTaskPriority.NORMAL),
                        task("Old done task", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.HIGH),
                        task("Ship deals page", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.CRITICAL)))),
                List.of());
    }

    private static TaskBoardPublicTaskView task(String title, TaskBoardTaskStatus status, TaskBoardTaskPriority priority) {
        return new TaskBoardPublicTaskView(UUID.randomUUID(), "tavall-times", "launch", title, "", status, priority,
                TaskBoardQaStatus.NHV, 1, "github-issue", Instant.now(), List.of());
    }
}
