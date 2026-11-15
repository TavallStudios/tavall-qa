package org.tavall.qa.web.pages.task;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaTaskDetailPage}: the real page composes the real
 * {@code QaTaskDetailPageHTML} builder with real domain views. No collaborators are faked.
 */
final class QaTaskDetailPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void nhvTaskRendersNotHumanVerifiedBadgeAndNoVerifiedClaim() {
        TaskBoardTaskView view = new TaskBoardTaskView(project(), task(TaskBoardQaStatus.NHV,
                TaskBoardTaskStatus.ACTIVE, null), List.of(), List.of(), List.of(), List.of(), List.of());

        String html = renderer.render(QaTaskDetailPage.build(view, false, false)).body();

        assertTrue(html.contains("Fix combo knockback vector"));
        assertTrue(html.contains("NHV — Not Human Verified"));
        assertFalse(html.contains("HUMAN VERIFIED"));
    }

    @Test
    void humanVerifiedTaskRendersVerifierInTheAuditTrail() {
        UUID verifierUuid = UUID.randomUUID();
        TaskBoardTaskView view = new TaskBoardTaskView(project(), task(TaskBoardQaStatus.HUMAN_VERIFIED,
                TaskBoardTaskStatus.DONE, verifierUuid), List.of(), List.of(), List.of(), List.of(), List.of());

        String html = renderer.render(QaTaskDetailPage.build(view, true, true)).body();

        assertTrue(html.contains("HUMAN VERIFIED"));
        assertTrue(html.contains(verifierUuid.toString()));
        assertTrue(html.contains("Staff workspace"));
        assertTrue(html.contains("Open Focus Workspace →"));
    }

    @Test
    void publicViewerDoesNotSeeExecutionInternalsEvenWhenTheyExist() {
        TaskBoardTaskView view = new TaskBoardTaskView(project(), task(TaskBoardQaStatus.NHV,
                TaskBoardTaskStatus.ACTIVE, null), List.of(), List.of(), List.of(), List.of(), List.of());
        QaTaskExecutionView execution = new QaTaskExecutionView(
                "internal-runner-7f3a", "internal-provider", UUID.randomUUID(),
                "none", "", List.of(), List.of(), true, true);

        String html = renderer.render(QaTaskDetailPage.build(view, false, false,
                QaTaskPageContext.readOnly().withExecution(execution))).body();

        assertFalse(html.contains("internal-runner-7f3a"));
        assertFalse(html.contains("internal-provider"));
        assertFalse(html.contains("Compile, Test &amp; CI Evidence"));
        assertTrue(html.contains("Fix combo knockback vector"));
    }

    @Test
    void missingTaskUsesGenericTitleAndStillRendersThePublicShell() {
        QaTaskDetailPage page = QaTaskDetailPage.build((TaskBoardTaskView) null, false, false);

        assertEquals("Task Detail | Tavall QA", page.title());
        assertTrue(renderer.render(page).body().contains("Task not found or unavailable."));
    }

    private static TaskBoardProject project() {
        Instant now = Instant.now();
        return new TaskBoardProject(
                "tavall-pvp", "Tavall PvP", "Competitive arena", TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC, 0, now, now
        );
    }

    private static TaskBoardTask task(TaskBoardQaStatus qaStatus, TaskBoardTaskStatus status, UUID verifierUuid) {
        Instant now = Instant.now();
        boolean verified = qaStatus == TaskBoardQaStatus.HUMAN_VERIFIED;
        UUID creator = UUID.randomUUID();
        return new TaskBoardTask(
                UUID.randomUUID(),
                "tavall-pvp",
                "ffa",
                "Fix combo knockback vector",
                "Fix combo knockback vector",
                "Internal description of vector mechanics",
                "Public summary of knockback",
                status,
                TaskBoardTaskPriority.HIGH,
                qaStatus,
                TaskBoardAudience.PUBLIC,
                3,
                true,
                "github",
                "https://github.com/TavallStudios/tavall-mc/issues/263",
                true,
                creator,
                UUID.randomUUID(),
                verified ? creator : null,
                verified ? verifierUuid : null,
                verified ? UUID.randomUUID() : null,
                verified ? now : null,
                0,
                now,
                now
        );
    }
}
