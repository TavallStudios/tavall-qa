package org.tavall.qa.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardQueryHandlerTest {
    private TaskBoardMutationHandler mutationHandler;
    private TaskBoardQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        TaskBoardTestDataHandler dataHandler = new TaskBoardTestDataHandler();
        TaskBoardActionTargetResolver resolver = new TaskBoardActionTargetResolver();
        mutationHandler = new TaskBoardMutationHandler(dataHandler, resolver);
        queryHandler = new TaskBoardQueryHandler(dataHandler, resolver);
        mutationHandler.createProject(new TaskBoardProjectCreateRequest(
                "project-novus",
                "Project Novus",
                "Public development board",
                TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC,
                10
        ));
    }

    @Test
    void publicProjectionExcludesPrivateTasksFromCountsAndProgress() {
        TaskBoardTask publicTask = createTask("website-foundation", TaskBoardAudience.PUBLIC, 3);
        createTask("website-foundation", TaskBoardAudience.ADMIN_ONLY, 50);
        complete(publicTask);

        var dashboard = queryHandler.publicDashboard();
        assertEquals(1, dashboard.projects().size());
        assertEquals(1, dashboard.projects().getFirst().taskCount());
        assertEquals(100, dashboard.projects().getFirst().progressPercent());
    }

    @Test
    void weightedProgressUsesVisibleTaskWeight() {
        TaskBoardTask heavy = createTask("kingdom-systems", TaskBoardAudience.PUBLIC, 3);
        createTask("kingdom-systems", TaskBoardAudience.PUBLIC, 1);
        complete(heavy);

        var project = queryHandler.findPublicProjectView("project-novus").orElseThrow();
        assertEquals(75, project.summary().progressPercent());
        assertEquals(75, project.sections().getFirst().progressPercent());
        assertEquals(4, project.sections().getFirst().totalWeight());
    }

    @Test
    void publicDashboardHighlightsActiveAndRecentlyCompletedTasks() {
        TaskBoardTask active = createTask("pvp-engine", TaskBoardAudience.PUBLIC, 2);
        mutationHandler.updateTask(updateRequest(
                active,
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardQaStatus.NHV
        ));
        TaskBoardTask done = createTask("pvp-engine", TaskBoardAudience.PUBLIC, 1);
        complete(done);

        var dashboard = queryHandler.publicDashboard();
        assertEquals(1, dashboard.activeTasks().size());
        assertEquals("Public title", dashboard.activeTasks().getFirst().title());
        assertEquals(1, dashboard.recentlyCompleted().size());
        assertEquals("Public title", dashboard.recentlyCompleted().getFirst().title());
    }

    @Test
    void nhvTasksReportedSeparatelyFromCompletedTasks() {
        TaskBoardTask doneNhv = createTask("build-pipeline", TaskBoardAudience.PUBLIC, 1);
        complete(doneNhv);

        var project = queryHandler.findPublicProjectView("project-novus").orElseThrow();
        assertEquals(1, project.summary().completedCount());
        assertEquals(1, project.summary().nhvCount());
        assertTrue(project.sections().getFirst().nhvCount() > 0);
    }

    private TaskBoardTask createTask(
            String sectionKey,
            TaskBoardAudience audience,
            int weight
    ) {
        return mutationHandler.createTask(new TaskBoardTaskCreateRequest(
                "project-novus",
                sectionKey,
                "Internal title",
                "Public title",
                "Internal description",
                "Public summary",
                TaskBoardTaskPriority.NORMAL,
                audience,
                weight,
                false,
                "github-pr",
                "TavallStudios/tavall-project-novus#263",
                null,
                0
        ));
    }

    private void complete(TaskBoardTask task) {
        mutationHandler.updateTask(updateRequest(
                task,
                TaskBoardTaskStatus.DONE,
                TaskBoardQaStatus.NHV
        ));
    }

    private TaskBoardTaskUpdateRequest updateRequest(
            TaskBoardTask task,
            TaskBoardTaskStatus status,
            TaskBoardQaStatus qaStatus
    ) {
        return new TaskBoardTaskUpdateRequest(
                task.taskId(),
                task.sectionKey(),
                task.title(),
                task.publicTitle(),
                task.description(),
                task.publicSummary(),
                status,
                task.priority(),
                qaStatus,
                task.audience(),
                task.weight(),
                task.hostValidationRequired(),
                task.sourceType(),
                task.sourceReference(),
                null,
                task.sortOrder()
        );
    }
}
