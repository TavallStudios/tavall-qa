package org.tavall.qa.web.pages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardEvidenceStatus;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicSectionView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.web.pages.main.QaMainBoardPageContext;
import org.tavall.qa.web.pages.workspace.QaWorkspacePageContext;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.qa.view.TaskBoardEvidenceView;
import org.tavall.qa.web.pages.task.QaTaskPageContext;
import org.tavall.qa.web.pages.editor.QaMessageEditorPage;
import org.tavall.qa.web.pages.main.QaMainBoardPage;
import org.tavall.qa.web.pages.project.QaProjectDetailPage;
import org.tavall.qa.web.pages.task.QaTaskDetailPage;
import org.tavall.qa.web.pages.workspace.QaStaffWorkspacePage;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QaPagesTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    @DisplayName("Main Board renders approved Direction C synthesis")
    void mainBoardDirectionCSynthesis() {
        TaskBoardPublicProjectSummaryView project = new TaskBoardPublicProjectSummaryView(
                "tavall-pvp",
                "Tavall PvP",
                "Competitive Minecraft PvP arena",
                TaskBoardProjectStatus.ACTIVE,
                75,
                10,
                3,
                5,
                2,
                1
        );

        TaskBoardPublicDashboardView dashboard = new TaskBoardPublicDashboardView(
                true,
                List.of(project),
                List.of(),
                75,
                10,
                3,
                5,
                2,
                List.of(),
                List.of()
        );

        QaMainBoardPage page = QaMainBoardPage.build(dashboard, true,
                new QaMainBoardPageContext("LINKED", java.util.Optional.empty(), "_csrf", "token", true,
                        java.util.Optional.empty(), List.of()));
        assertNotNull(page);
        assertTrue(page.animations().animations().stream()
                .anyMatch(animation -> animation.invocation().function().value().equals("initializeQaBoard")));

        String html = renderer.render(page).body();
        assertTrue(html.contains("Follow what changed."));
        assertTrue(html.contains("Public development tracker"));
        assertTrue(html.contains("Tavall PvP"));
        assertTrue(html.contains("Staff mode"));
        assertTrue(html.contains("75%"));
    }

    @Test
    @DisplayName("Project Detail renders Direction A system hierarchy and expander")
    void projectDetailDirectionA() {
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

    @Test
    @DisplayName("Task Detail renders Direction A evidence layout and NHV badge")
    void taskDetailDirectionAAndNhv() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0, now, now
        );

        TaskBoardTask taskNhv = new TaskBoardTask(
                taskId,
                "tavall-pvp",
                "ffa",
                "Fix combo knockback vector",
                "Fix combo knockback vector",
                "Internal description of vector mechanics",
                "Public summary of knockback",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263",
                false,
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

        TaskBoardTaskView viewNhv = new TaskBoardTaskView(project, taskNhv, List.of(), List.of(), List.of(), List.of(), List.of());
        QaTaskDetailPage pageNhv = QaTaskDetailPage.build(viewNhv, false, false);
        String htmlNhv = renderer.render(pageNhv).body();

        assertTrue(htmlNhv.contains("Fix combo knockback vector"));
        assertTrue(htmlNhv.contains("NHV — Not Human Verified"));
        assertFalse(htmlNhv.contains("HUMAN VERIFIED"));

        // Now human verified
        UUID verifierUuid = UUID.randomUUID();
        TaskBoardTask taskVerified = new TaskBoardTask(
                taskId,
                "tavall-pvp",
                "ffa",
                "Fix combo knockback vector",
                "Fix combo knockback vector",
                "Internal description of vector mechanics",
                "Public summary of knockback",
                TaskBoardTaskStatus.DONE,
                TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.HUMAN_VERIFIED,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263",
                false,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                verifierUuid,
                UUID.randomUUID(),
                now,
                0,
                now,
                now
        );

        TaskBoardTaskView viewVerified = new TaskBoardTaskView(project, taskVerified, List.of(), List.of(), List.of(), List.of(), List.of());
        QaTaskDetailPage pageVerified = QaTaskDetailPage.build(viewVerified, true, true);
        String htmlVerified = renderer.render(pageVerified).body();

        assertTrue(htmlVerified.contains("HUMAN VERIFIED"));
        assertTrue(htmlVerified.contains(verifierUuid.toString()));
    }

    @Test
    @DisplayName("Staff Focus Workspace renders Direction C modes and panels")
    void staffWorkspaceDirectionC() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask workspaceTask = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Implement ranked queue", "Implement ranked queue",
                "Task description", "Summary", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.CRITICAL,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 5, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now);
        TaskBoardTaskView workspaceView = new TaskBoardTaskView(project, workspaceTask, List.of(), List.of(), List.of(), List.of(), List.of());
        QaWorkspacePageContext workspaceContext = new QaWorkspacePageContext(
                "QA", "CURRENT", "_csrf", "csrf-token", java.util.Optional.of(UUID.randomUUID()),
                Set.of(QaEnvironmentGrant.DEV), Set.of(QaEnvironmentGrant.DEV), Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaEnvironmentGrant.DEV), Set.of(), true, true, true);

        QaStaffWorkspacePage page = QaStaffWorkspacePage.build(workspaceView, workspaceContext);
        assertTrue(page.animations().animations().stream()
                .anyMatch(animation -> animation.invocation().function().value().equals("initializeQaBoard")));
        String html = renderer.render(page).body();

        assertTrue(html.contains("Direction C · Focus Workspace"));
        assertTrue(html.contains("Overview"));
        assertTrue(html.contains("Qa"));
        assertTrue(html.contains("Edit"));
        assertTrue(html.contains("Thread"));
        assertTrue(html.contains("Submit evidence"));
        assertTrue(html.contains("Save task"));
        assertFalse(html.contains("Reject task acceptance"));
        assertTrue(html.contains("method=\"post\""));
        assertTrue(html.contains("data-tavall-qa-workspace-focus=\"QA\""));
        assertTrue(html.contains("data-tavall-qa-workspace-panel=\"THREAD\""));
    }

    @Test
    @DisplayName("Task acceptance uses current accepted evidence and keeps the task NHV")
    void taskAcceptanceFormsRequireCurrentAcceptedEvidence() {
        UUID taskId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, project.projectKey(), "ffa", "Accepted task", "Accepted task", "Requirements",
                "Public summary", TaskBoardTaskStatus.REVIEW, TaskBoardTaskPriority.NORMAL,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 3, true, "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263", true, 0,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now
        );
        TaskBoardEvidence evidence = new TaskBoardEvidence(
                evidenceId, taskId, UUID.randomUUID(), UUID.randomUUID(), Set.of(QaClass.REGULAR),
                java.util.Optional.of(QaTier.TIER_I), QaEnvironmentGrant.DEV, "build-1", "current task test",
                "Expected behavior", "Observed behavior", "https://example.com/evidence", "Safe summary", true,
                org.tavall.qa.domain.TaskBoardTaskFingerprint.compute(task), TaskBoardEvidenceStatus.ACCEPTED,
                UUID.randomUUID(), "Accepted", now, now
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(
                project, task, List.of(), List.of(), List.of(new TaskBoardEvidenceView(evidence)), List.of(), List.of()
        );
        UUID organizationId = UUID.randomUUID();
        QaTaskPageContext taskContext = new QaTaskPageContext(
                "_csrf", "csrf-token", java.util.Optional.of(organizationId), Set.of(), Set.of(), Set.of(),
                Set.of(QaEnvironmentGrant.DEV), Set.of()
        );
        String taskHtml = renderer.render(QaTaskDetailPage.build(taskView, false, false, taskContext)).body();
        assertTrue(taskHtml.contains("/accept"));
        assertTrue(taskHtml.contains("Accept task · remains NHV"));
        assertTrue(taskHtml.contains(evidenceId.toString()));
        assertTrue(taskHtml.contains(organizationId.toString()));

        QaWorkspacePageContext workspaceContext = new QaWorkspacePageContext(
                "QA", "CURRENT", "_csrf", "csrf-token", java.util.Optional.of(organizationId),
                Set.of(), Set.of(), Set.of(QaEnvironmentGrant.DEV), Set.of(), Set.of(), true, true, false
        );
        String workspaceHtml = renderer.render(QaStaffWorkspacePage.build(taskView, workspaceContext)).body();
        assertTrue(workspaceHtml.contains("/accept"));
        assertTrue(workspaceHtml.contains("Accept task with this evidence"));
        assertTrue(workspaceHtml.contains("keeps it NHV"));

        TaskBoardTask revisedTask = new TaskBoardTask(
                task.taskId(), task.projectKey(), task.sectionKey(), task.title(), task.publicTitle(),
                task.description(), task.publicSummary(), task.status(), task.priority(), task.qaStatus(),
                task.audience(), task.weight(), task.hostValidationRequired(), task.sourceType(),
                task.sourceReference(), task.qaQcEligible(), task.qaRevision() + 1, task.createdByAccountId(),
                task.createdByMinecraftUUID(), null, null, null, null, task.sortOrder(), task.createdAt(), now
        );
        TaskBoardTaskView revisedView = new TaskBoardTaskView(
                project, revisedTask, List.of(), List.of(), List.of(new TaskBoardEvidenceView(evidence)), List.of(), List.of()
        );
        String staleWorkspaceHtml = renderer.render(QaStaffWorkspacePage.build(revisedView, workspaceContext)).body();
        assertFalse(staleWorkspaceHtml.contains("Accept task with this evidence"));
    }

    @Test
    @DisplayName("Message Editor renders Desktop Direction A and Mobile Direction C")
    void messageEditorDesktopAMobileC() {
        QaMessageEditorPage page = QaMessageEditorPage.build(
                "ffa.match.countdown",
                "en_us",
                "UNIVERSAL",
                "TITLE",
                "<gold>Match starts in {{countdown.seconds}}</gold>",
                "/projects/tavall-pvp/tasks/test-task"
        );

        String html = renderer.render(page).body();
        assertTrue(html.contains("Minecraft Preview"));
        assertTrue(html.contains("PREVIEW"));
        assertTrue(html.contains("EDIT"));
        assertTrue(html.contains("MESSAGES"));
        assertTrue(html.contains("Match starts in 5"));
        assertTrue(html.contains("Save &amp; Return to Task"));
    }
}
