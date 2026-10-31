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
import org.tavall.qa.web.pages.task.QaTaskExecutionView;
import org.tavall.qa.web.pages.task.QaTaskAgentContextView;
import org.tavall.qa.web.pages.task.QaTaskExecutionCheckpointView;
import org.tavall.qa.web.pages.task.QaTaskExecutionEvidenceLink;
import org.tavall.qa.web.pages.task.QaExecutionEvidenceType;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.qa.web.pages.main.QaMainBoardPage;
import org.tavall.qa.web.pages.project.QaProjectDetailPage;
import org.tavall.qa.web.pages.task.QaTaskDetailPage;
import org.tavall.qa.web.pages.workspace.QaStaffWorkspacePage;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        assertTrue(page.animations().animations().stream()
                .anyMatch(animation -> animation.invocation().function().value().equals("initializeQaBoard")));
        assertTrue(html.contains("Tavall PvP"));
        assertTrue(html.contains("Show more tasks"));
        assertTrue(html.contains("data-tavall-qa-task-group=\"ffa\""));
        assertTrue(html.contains("data-tavall-qa-task-item=\"" + taskId1 + "\""));
        assertTrue(html.contains("data-tavall-qa-task-expander=\"ffa\""));
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
    @DisplayName("Task Detail execution section renders diagnostics and actions for authorized staff")
    void taskDetailExecutionSectionRenderedForAuthorizedStaff() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        UUID checkpointId1 = UUID.randomUUID();
        UUID checkpointId2 = UUID.randomUUID();
        Instant now = Instant.now();

        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Fix knockback vector", "Fix knockback vector",
                "Internal description", "Summary", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 3, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of());

        QaTaskExecutionCheckpointView cp1 = new QaTaskExecutionCheckpointView(
                checkpointId1, 1, "codex-tj", "QUEUED", now.minusSeconds(300), "Initialize worktree lease"
        );
        QaTaskExecutionCheckpointView cp2 = new QaTaskExecutionCheckpointView(
                checkpointId2, 2, "codex-tj", "RUNNING", now.minusSeconds(120), "Executing unit tests"
        );

        List<QaTaskExecutionEvidenceLink> links = List.of(
                QaTaskExecutionEvidenceLink.compile("Gradle compileJava logs", "https://ci.tavall.org/builds/9001/compile"),
                QaTaskExecutionEvidenceLink.test("JUnit test results", "https://ci.tavall.org/builds/9001/test"),
                QaTaskExecutionEvidenceLink.ci("GitHub Actions CI run", "https://github.com/TavallStudios/tavall-web-qa/actions/runs/12345")
        );

        QaTaskExecutionView execution = new QaTaskExecutionView(
                "operator-401",
                "codex-tj",
                runId,
                "Tier-2 Codex API limit: 80% quota utilized",
                "Awaiting compilation evidence check",
                List.of(cp1, cp2),
                links,
                true,
                true,
                List.of()
        );

        QaTaskPageContext context = new QaTaskPageContext(
                "_csrf", "test-csrf-token", java.util.Optional.empty(), Set.of(),
                Set.of(QaEnvironmentGrant.DEV), Set.of(QaEnvironmentGrant.DEV), Set.of(), Set.of(),
                java.util.Optional.of(execution)
        );

        QaTaskDetailPage page = QaTaskDetailPage.build(taskView, true, true, context);
        String html = renderer.render(page).body();

        // 1. Header and metadata
        assertTrue(html.contains("Execution &amp; Diagnostics") || html.contains("Execution & Diagnostics"));
        assertTrue(html.contains("Lease Holder"));
        assertTrue(html.contains("operator-401"));
        assertTrue(html.contains("Provider ID"));
        assertTrue(html.contains("codex-tj"));
        assertTrue(html.contains("Run ID"));
        assertTrue(html.contains(runId.toString()));
        assertTrue(html.contains("Quota Restriction"));
        assertTrue(html.contains("Tier-2 Codex API limit: 80% quota utilized"));
        assertTrue(html.contains("Blocked Reason"));
        assertTrue(html.contains("Awaiting compilation evidence check"));

        // 2. Checkpoints
        assertTrue(html.contains("Ordered Checkpoints"));
        assertTrue(html.contains("Checkpoint #1 · QUEUED"));
        assertTrue(html.contains("Checkpoint #2 · RUNNING"));
        assertTrue(html.contains("Next action: Initialize worktree lease"));
        assertTrue(html.contains("Next action: Executing unit tests"));
        assertTrue(html.contains(checkpointId1.toString()));
        assertTrue(html.contains(checkpointId2.toString()));

        // 3. Evidence links
        assertTrue(html.contains("Compile, Test &amp; CI Evidence") || html.contains("Compile, Test & CI Evidence"));
        assertTrue(html.contains("Gradle compileJava logs"));
        assertTrue(html.contains("https://ci.tavall.org/builds/9001/compile"));
        assertTrue(html.contains("JUnit test results"));
        assertTrue(html.contains("https://ci.tavall.org/builds/9001/test"));
        assertTrue(html.contains("GitHub Actions CI run"));
        assertTrue(html.contains("https://github.com/TavallStudios/tavall-web-qa/actions/runs/12345"));

        // 4. Action buttons and routes (following server-side mutation pattern)
        assertTrue(html.contains("/projects/tavall-pvp/tasks/" + taskId + "/resume"));
        assertTrue(html.contains("/projects/tavall-pvp/tasks/" + taskId + "/lease/release"));
        assertTrue(html.contains("Resume execution"));
        assertTrue(html.contains("Cancel execution"));
        assertFalse(html.contains("Retry execution"));
        assertTrue(html.contains("test-csrf-token"));
    }

    @Test
    @DisplayName("Public views must not show execution internals")
    void publicViewsDoNotShowExecutionInternals() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        Instant now = Instant.now();

        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Fix knockback vector", "Fix knockback vector",
                "Internal description", "Summary", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 3, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of());

        QaTaskExecutionView execution = new QaTaskExecutionView(
                "secret-operator-uuid",
                "internal-provider-xyz",
                runId,
                "Internal rate limit",
                "Secret blocked reason",
                List.of(new QaTaskExecutionCheckpointView(UUID.randomUUID(), 1, "internal-provider-xyz", "RUNNING", now, "Run private step")),
                List.of(QaTaskExecutionEvidenceLink.ci("Internal CI", "https://private-ci.internal/build/1")),
                true,
                true,
                List.of()
        );

        // Public read-only context (no staff/reviewer/contributor roles)
        QaTaskPageContext publicContext = QaTaskPageContext.readOnly().withExecution(execution);

        QaTaskDetailPage page = QaTaskDetailPage.build(taskView, false, false, publicContext);
        String html = renderer.render(page).body();

        // Must not contain any execution internals
        assertFalse(html.contains("Execution & Diagnostics"));
        assertFalse(html.contains("Execution &amp; Diagnostics"));
        assertFalse(html.contains("secret-operator-uuid"));
        assertFalse(html.contains("internal-provider-xyz"));
        assertFalse(html.contains("Internal rate limit"));
        assertFalse(html.contains("Secret blocked reason"));
        assertFalse(html.contains("Run private step"));
        assertFalse(html.contains("private-ci.internal"));
        assertFalse(html.contains("/resume"));
        assertFalse(html.contains("/lease/release"));
        assertFalse(html.contains("/retry"));
    }

    @Test
    @DisplayName("Execution actions respect role checks: contributor cannot cancel, reviewer and admin can")
    void executionActionsRespectRoleChecks() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();

        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, Instant.now(), Instant.now()
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Fix knockback vector", "Fix knockback vector",
                "Internal description", "Summary", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 3, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, Instant.now(), Instant.now()
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of());

        QaTaskExecutionView execution = new QaTaskExecutionView(
                "worker-1", "codex", runId, null, null, List.of(), List.of(), true, true, List.of()
        );

        // 1. Contributor (evidence submission grant, but not reviewer or admin)
        QaTaskPageContext contributorContext = new QaTaskPageContext(
                "_csrf", "token", java.util.Optional.empty(), Set.of(),
                Set.of(QaEnvironmentGrant.DEV), Set.of(), Set.of(), Set.of(),
                java.util.Optional.of(execution)
        );
        QaTaskDetailPage contributorPage = QaTaskDetailPage.build(taskView, false, false, contributorContext);
        String contributorHtml = renderer.render(contributorPage).body();

        assertTrue(contributorHtml.contains("Resume execution"));
        assertFalse(contributorHtml.contains("Cancel execution"));
        assertFalse(contributorHtml.contains("Retry execution"));

        // 2. QA Reviewer (review grant)
        QaTaskPageContext reviewerContext = new QaTaskPageContext(
                "_csrf", "token", java.util.Optional.empty(), Set.of(),
                Set.of(), Set.of(QaEnvironmentGrant.DEV), Set.of(), Set.of(),
                java.util.Optional.of(execution)
        );
        QaTaskDetailPage reviewerPage = QaTaskDetailPage.build(taskView, false, false, reviewerContext);
        String reviewerHtml = renderer.render(reviewerPage).body();

        assertTrue(reviewerHtml.contains("Resume execution"));
        assertTrue(reviewerHtml.contains("Cancel execution"));
        assertFalse(reviewerHtml.contains("Retry execution"));

        // 3. Admin (canManage = true)
        QaTaskPageContext adminContext = new QaTaskPageContext(
                "_csrf", "token", java.util.Optional.empty(), Set.of(),
                Set.of(), Set.of(), Set.of(), Set.of(),
                java.util.Optional.of(execution)
        );
        QaTaskDetailPage adminPage = QaTaskDetailPage.build(taskView, true, false, adminContext);
        String adminHtml = renderer.render(adminPage).body();

        assertTrue(adminHtml.contains("Resume execution"));
        assertTrue(adminHtml.contains("Cancel execution"));
        assertFalse(adminHtml.contains("Retry execution"));
    }

    @Test
    @DisplayName("Checkpoints are rendered in ascending sequence order regardless of input order")
    void checkpointsRenderedInAscendingOrder() {
        UUID taskId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        Instant now = Instant.now();

        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Task", "Task",
                "Desc", "Sum", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.NORMAL,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 1, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of());

        // Pass checkpoints in unordered sequence: 5, 1, 3
        QaTaskExecutionCheckpointView cp5 = new QaTaskExecutionCheckpointView(UUID.randomUUID(), 5, "p1", "RUNNING", now, "Step 5");
        QaTaskExecutionCheckpointView cp1 = new QaTaskExecutionCheckpointView(UUID.randomUUID(), 1, "p1", "QUEUED", now, "Step 1");
        QaTaskExecutionCheckpointView cp3 = new QaTaskExecutionCheckpointView(UUID.randomUUID(), 3, "p1", "RUNNING", now, "Step 3");

        QaTaskExecutionView execution = new QaTaskExecutionView(
                "worker", "p1", runId, null, null, List.of(cp5, cp1, cp3), List.of(), false, false, List.of()
        );

        QaTaskDetailPage page = QaTaskDetailPage.build(taskView, true, false, QaTaskPageContext.readOnly(), execution);
        String html = renderer.render(page).body();

        int pos1 = html.indexOf("Checkpoint #1");
        int pos3 = html.indexOf("Checkpoint #3");
        int pos5 = html.indexOf("Checkpoint #5");

        assertTrue(pos1 > 0 && pos3 > 0 && pos5 > 0);
        assertTrue(pos1 < pos3);
        assertTrue(pos3 < pos5);
    }

    @Test
    @DisplayName("QaRoutes execution routes and helpers match backend contracts")
    void executionRoutesMatchBackendContracts() {
        UUID taskId = UUID.randomUUID();
        String projectKey = "tavall-pvp";

        assertEquals("/projects/tavall-pvp/tasks/" + taskId + "/resume", QaRoutes.executionResume(projectKey, taskId));
        assertEquals("/projects/tavall-pvp/tasks/" + taskId + "/lease/release", QaRoutes.executionRelease(projectKey, taskId));
        assertEquals("/projects/tavall-pvp/tasks/" + taskId + "/lease/release", QaRoutes.executionCancel(projectKey, taskId));

        assertEquals("/projects/{projectKey}/tasks/{taskId}/resume", QaRoutes.EXECUTION_RESUME_MUTATION);
        assertEquals("/projects/{projectKey}/tasks/{taskId}/lease/release", QaRoutes.EXECUTION_RELEASE_MUTATION);
        assertEquals("/projects/{projectKey}/tasks/{taskId}/lease/release", QaRoutes.EXECUTION_CANCEL_MUTATION);

        assertEquals("/api/execution/tasks/" + taskId, QaRoutes.backendExecutionTask(taskId));
        assertEquals("/api/execution/tasks/" + taskId + "/lease", QaRoutes.backendExecutionLease(taskId));
        assertEquals("/api/execution/tasks/" + taskId + "/lease/release", QaRoutes.backendExecutionRelease(taskId));
        assertEquals("/api/execution/tasks/" + taskId + "/checkpoints", QaRoutes.backendExecutionCheckpoints(taskId));
        assertEquals("/api/execution/tasks/" + taskId + "/resume", QaRoutes.backendExecutionResume(taskId));

    }

    @Test
    @DisplayName("Agent context viewer renders two agents with distinct colors, escaped content, bounded diff preview and empty state")
    void agentContextViewerRenderingWithTwoAgentsAndEmptyState() {
        UUID taskId = UUID.randomUUID();
        Instant now = Instant.now();
        TaskBoardProject project = new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
        TaskBoardTask task = new TaskBoardTask(
                taskId, "tavall-pvp", "ffa", "Task", "Task",
                "Desc", "Sum", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.NORMAL,
                TaskBoardQaStatus.NHV, TaskBoardAudience.PUBLIC, 1, false, "github", "", false,
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, 0, now, now
        );
        TaskBoardTaskView taskView = new TaskBoardTaskView(project, task, List.of(), List.of(), List.of(), List.of(), List.of());

        // 1. Empty state
        QaTaskExecutionView emptyExecution = new QaTaskExecutionView(
                "worker", "p1", UUID.randomUUID(), null, null, List.of(), List.of(), false, false, List.of()
        );
        QaTaskDetailPage emptyPage = QaTaskDetailPage.build(taskView, true, false, QaTaskPageContext.readOnly(), emptyExecution);
        String emptyHtml = renderer.render(emptyPage).body();
        assertTrue(emptyHtml.contains("Live Agent Contexts"));
        assertTrue(emptyHtml.contains("No agent contexts recorded."));

        // 2. Two agents with distinct colors, escaped content, and bounded diff preview
        UUID agentAccount1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID agentAccount2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID runId1 = UUID.randomUUID();
        UUID runId2 = UUID.randomUUID();

        String color1 = QaTaskAgentContextView.colorTokenFor(agentAccount1);
        String color2 = QaTaskAgentContextView.colorTokenFor(agentAccount2);
        assertNotNull(color1);
        assertNotNull(color2);
        assertFalse(color1.equals(color2), "Two agents should have distinct colors");

        // Prepare large diff > 16 KiB
        String largePatch = "+ added line\n".repeat(2000);
        assertTrue(largePatch.length() > 16 * 1024);

        // Prepare more than 64 files to verify bounding
        List<String> manyFiles = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "file-" + i + ".txt")
                .toList();

        QaTaskAgentContextView agent1 = new QaTaskAgentContextView(
                agentAccount1,
                "codex-tj",
                color1,
                runId1,
                "RUNNING",
                now.minusSeconds(60),
                "Executing <script>alert('xss')</script> & checking files",
                manyFiles,
                List.of("src/Main.java", "build.gradle"),
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                largePatch,
                List.of("docs/README.md", "docs/AUTH.md"),
                List.of("evidence://ref-1")
        );

        // Verify bounds on record
        assertEquals(QaTaskAgentContextView.MAX_LIST_ENTRIES, agent1.inspectedFiles().size());
        assertTrue(agent1.diffPreview().contains("truncated to 16 KiB"));

        QaTaskAgentContextView agent2 = new QaTaskAgentContextView(
                agentAccount2,
                "claude-opus",
                color2,
                runId2,
                "COMPLETED",
                now.minusSeconds(10),
                "Finalizing code review & validation",
                List.of("tests/Test.java"),
                List.of(),
                null,
                "",
                List.of(),
                List.of()
        );

        QaTaskExecutionView execution = new QaTaskExecutionView(
                "operator", "multi-agent", runId1, null, null, List.of(), List.of(), true, true,
                List.of(agent1, agent2)
        );

        QaTaskDetailPage page = QaTaskDetailPage.build(taskView, true, false, QaTaskPageContext.readOnly(), execution);
        String html = renderer.render(page).body();

        // Check agents are present
        assertTrue(html.contains("Live Agent Contexts"));
        assertFalse(html.contains("No agent contexts recorded."));

        // Colors
        assertTrue(html.contains(color1));
        assertTrue(html.contains(color2));

        // Agent labels and statuses
        assertTrue(html.contains("codex-tj · RUNNING"));
        assertTrue(html.contains("claude-opus · COMPLETED"));

        // Agent account IDs and Run IDs
        assertTrue(html.contains(agentAccount1.toString()));
        assertTrue(html.contains(agentAccount2.toString()));
        assertTrue(html.contains(runId1.toString()));
        assertTrue(html.contains(runId2.toString()));

        // Escaped content
        assertTrue(html.contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; &amp; checking files"));
        assertFalse(html.contains("<script>alert('xss')</script>"));

        // Bounded preview rendered
        assertTrue(html.contains("truncated to 16 KiB"));
        assertTrue(html.contains("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        // Files and references rendered
        assertTrue(html.contains("src/Main.java"));
        assertTrue(html.contains("docs/README.md"));
        assertTrue(html.contains("evidence://ref-1"));
        assertTrue(html.contains("tests/Test.java"));
    }
}
