package org.tavall.qa.web.pages;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
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
import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.qa.web.pages.editor.QaMessageEditorPage;
import org.tavall.qa.web.pages.main.QaMainBoardPage;
import org.tavall.qa.web.pages.project.QaProjectDetailPage;
import org.tavall.qa.web.pages.task.QaTaskDetailPage;
import org.tavall.qa.web.pages.workspace.QaStaffWorkspacePage;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
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

        QaMainBoardPage page = QaMainBoardPage.build(dashboard, true, true, "LINKED");
        assertNotNull(page);

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

        TaskBoardPublicTaskView t1 = new TaskBoardPublicTaskView(taskId1, "tavall-pvp", "ffa", "Task 1", "Summary 1", TaskBoardTaskStatus.DONE, TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.HUMAN_VERIFIED, 1, "github", Instant.now());
        TaskBoardPublicTaskView t2 = new TaskBoardPublicTaskView(taskId2, "tavall-pvp", "ffa", "Task 2", "Summary 2", TaskBoardTaskStatus.ACTIVE, TaskBoardTaskPriority.HIGH, TaskBoardQaStatus.NHV, 2, "github", Instant.now());
        TaskBoardPublicTaskView t3 = new TaskBoardPublicTaskView(taskId3, "tavall-pvp", "ffa", "Task 3", "Summary 3", TaskBoardTaskStatus.BACKLOG, TaskBoardTaskPriority.LOW, TaskBoardQaStatus.NHV, 1, "github", Instant.now());
        TaskBoardPublicTaskView t4 = new TaskBoardPublicTaskView(taskId4, "tavall-pvp", "ffa", "Task 4", "Summary 4", TaskBoardTaskStatus.BACKLOG, TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV, 1, "github", Instant.now());

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
                UUID.randomUUID(),
                null,
                null,
                0,
                now,
                now
        );

        TaskBoardTaskView viewNhv = new TaskBoardTaskView(project, taskNhv, List.of(), List.of());
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
                UUID.randomUUID(),
                verifierUuid,
                now,
                0,
                now,
                now
        );

        TaskBoardTaskView viewVerified = new TaskBoardTaskView(project, taskVerified, List.of(), List.of());
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
        TaskBoardTaskPageView task = new TaskBoardTaskPageView(
                taskId,
                "tavall-pvp",
                "Tavall PvP",
                "ffa",
                "Implement ranked queue",
                "Summary",
                TaskBoardTaskStatus.ACTIVE,
                TaskBoardTaskPriority.CRITICAL,
                TaskBoardQaStatus.NHV,
                TaskBoardAudience.PUBLIC,
                5,
                "github",
                now,
                true
        );

        QaStaffWorkspacePage page = QaStaffWorkspacePage.build(task, "QA", "CURRENT", true, "verifier-uuid");
        String html = renderer.render(page).body();

        assertTrue(html.contains("Direction C · Focus Workspace"));
        assertTrue(html.contains("Overview"));
        assertTrue(html.contains("Qa"));
        assertTrue(html.contains("Edit"));
        assertTrue(html.contains("Thread"));
        assertTrue(html.contains("Record QA Decision"));
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
