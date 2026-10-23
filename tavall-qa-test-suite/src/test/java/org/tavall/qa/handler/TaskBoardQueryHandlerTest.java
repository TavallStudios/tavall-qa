package org.tavall.qa.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaSubjectStatus;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardQueryHandlerTest {
    private TaskBoardTestDataHandler dataHandler;
    private TaskBoardMutationHandler mutationHandler;
    private TaskBoardQueryHandler queryHandler;
    private QaSubjectContext manager;

    @BeforeEach
    void setUp() {
        dataHandler = new TaskBoardTestDataHandler();
        TaskBoardActionTargetResolver resolver = new TaskBoardActionTargetResolver();
        mutationHandler = new TaskBoardMutationHandler(dataHandler, resolver);
        queryHandler = new TaskBoardQueryHandler(dataHandler, resolver);
        manager = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        mutationHandler.createProject(manager, new TaskBoardProjectCreateRequest(
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
        mutationHandler.updateTask(manager, updateStatus(active, TaskBoardTaskStatus.ACTIVE));
        TaskBoardTask done = createTask("pvp-engine", TaskBoardAudience.PUBLIC, 1);
        complete(done);

        var dashboard = queryHandler.publicDashboard();
        assertEquals(1, dashboard.activeTasks().size());
        assertEquals("Public title", dashboard.activeTasks().getFirst().title());
        assertEquals(1, dashboard.recentlyCompleted().size());
        assertEquals("Public title", dashboard.recentlyCompleted().getFirst().title());
    }

    @Test
    void ordinaryAccountCanReadPublicProjectionButNeverStaffProjection() {
        TaskBoardTask task = createTask("private-qa", TaskBoardAudience.PUBLIC, 1);
        QaSubjectContext ordinary = new QaSubjectContext(
                Optional.of(UUID.randomUUID()),
                QaSubjectStatus.ACTIVE,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                false
        );

        assertTrue(queryHandler.findPublicTaskView(task.taskId()).isPresent());
        assertTrue(queryHandler.findTaskView(task.taskId(), ordinary).isEmpty());
        var publicTask = queryHandler.findPublicTaskView(task.taskId()).orElseThrow();
        assertEquals("Public title", publicTask.title());
        assertEquals("Public summary", publicTask.summary());
    }

    @Test
    void audienceIsFilteredBeforeAuthorizedDashboardCounts() {
        createTask("website-foundation", TaskBoardAudience.PUBLIC, 2);
        createTask("website-foundation", TaskBoardAudience.STAFF, 20);
        QaSubjectContext ordinary = new QaSubjectContext(
                Optional.of(UUID.randomUUID()),
                QaSubjectStatus.ACTIVE,
                Optional.empty(),
                Optional.empty(),
                false,
                false,
                Set.of(),
                Optional.empty(),
                Set.of(),
                Set.of(),
                List.of(),
                false
        );

        var dashboard = queryHandler.dashboard(ordinary);
        assertEquals(1, dashboard.projects().size());
        assertEquals(1, dashboard.projects().getFirst().taskCount());
    }

    @Test
    void ownVqcProjectionShowsContributionWithoutGrantingPromotion() {
        UUID accountId = manager.accountId().orElseThrow();
        var progression = queryHandler.findQaProgression(accountId, manager).orElseThrow();

        assertEquals(0, progression.acceptedQaQcTaskCount());
        assertEquals(0, progression.vqcEarned());
        assertEquals(Optional.of(org.tavall.qa.domain.auth.QaTier.TIER_V), progression.currentTier());
        assertTrue(progression.nextTierVqcGuideline().isEmpty());
    }

    private TaskBoardTask createTask(String sectionKey, TaskBoardAudience audience, int weight) {
        return mutationHandler.createTask(manager, new TaskBoardTaskCreateRequest(
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
                false,
                "github-pr",
                "TavallStudios/tavall-project-novus#263",
                0
        ));
    }

    private void complete(TaskBoardTask task) {
        mutationHandler.updateTask(manager, updateStatus(task, TaskBoardTaskStatus.DONE));
    }

    private TaskBoardTaskUpdateRequest updateStatus(TaskBoardTask task, TaskBoardTaskStatus status) {
        return new TaskBoardTaskUpdateRequest(
                task.taskId(), null, null, null, null, null,
                status, null, null, null, null, null, null, null, null, null
        );
    }
}
