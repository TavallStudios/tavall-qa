package org.tavall.qa.web.pages.main;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaMainBoardPage}: the real page composes the real
 * {@code QaMainBoardPageHTML} builder and the shared Forge chrome through production wiring.
 * No collaborators are faked.
 */
final class QaMainBoardPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDirectionCSynthesis() {
        String html = renderer.render(QaMainBoardPage.build(availableDashboard(), true,
                QaMainBoardPageContext.anonymous())).body();

        assertNotNull(html);
        assertTrue(html.contains("Follow what changed."));
        assertTrue(html.contains("Public development tracker"));
        assertTrue(html.contains("Tavall PvP"));
        assertTrue(html.contains("Staff mode"));
        assertTrue(html.contains("75%"));
        assertTrue(html.contains("/projects/tavall-pvp"));
    }

    @Test
    void anonymousViewerWithoutManageRightsSeesNoStaffControls() {
        String html = renderer.render(QaMainBoardPage.build(availableDashboard(), false,
                QaMainBoardPageContext.anonymous())).body();

        assertTrue(html.contains("Public development tracker"));
        assertFalse(html.contains("Staff mode"));
        assertFalse(html.contains("Create project ↓"));
        assertFalse(html.contains("name=\"_csrf\""));
    }

    @Test
    void manageProjectsContextRendersCsrfProtectedCreateForm() {
        UUID organizationId = UUID.randomUUID();
        QaMainBoardPageContext context = new QaMainBoardPageContext(
                "AUTHENTICATED", Optional.of(organizationId), "_csrf", "board-csrf-token",
                true, Optional.empty(), List.of());

        String html = renderer.render(QaMainBoardPage.build(availableDashboard(), false, context)).body();

        assertTrue(html.contains("Staff mode"));
        assertTrue(html.contains("AUTHENTICATED"));
        assertTrue(html.contains("Create project ↓"));
        assertTrue(html.contains("Create Task Board project"));
        assertTrue(html.contains("name=\"_csrf\" value=\"board-csrf-token\""));
        assertTrue(html.contains("name=\"organizationId\" value=\"" + organizationId + "\""));
        assertTrue(html.contains("action=\"/projects\""));
    }

    @Test
    void unavailableStorageShowsNoticeInsteadOfProjectGrid() {
        TaskBoardPublicDashboardView unavailable = new TaskBoardPublicDashboardView(
                false, List.of(), List.of(), 0, 0, 0, 0, 0, List.of(), List.of());

        String html = renderer.render(QaMainBoardPage.build(unavailable, false,
                QaMainBoardPageContext.anonymous())).body();

        assertTrue(html.contains("Task Board storage is temporarily unavailable."));
        assertFalse(html.contains("Public work."));
        assertFalse(html.contains("Tavall PvP"));
    }

    @Test
    void emptyPublishedBoardStatesThatNoProjectsExistYet() {
        TaskBoardPublicDashboardView empty = new TaskBoardPublicDashboardView(
                true, List.of(), List.of(), 0, 0, 0, 0, 0, List.of(), List.of());

        String html = renderer.render(QaMainBoardPage.build(empty, false,
                QaMainBoardPageContext.anonymous())).body();

        assertTrue(html.contains("No public projects are published yet."));
        assertTrue(html.contains("Public work."));
    }

    private static TaskBoardPublicDashboardView availableDashboard() {
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

        return new TaskBoardPublicDashboardView(
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
    }
}
