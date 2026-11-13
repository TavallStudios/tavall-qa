package org.tavall.qa.web.pages.main;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectSummaryView;
import org.tavall.qa.web.pages.main.QaMainBoardPageContext;
import org.tavall.web.frontend.render.HTMLRenderer;

import java.util.List;

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

        QaMainBoardPage page = QaMainBoardPage.build(dashboard, true, QaMainBoardPageContext.anonymous());
        assertNotNull(page);

        String html = renderer.render(page).body();
        assertTrue(html.contains("Follow what changed."));
        assertTrue(html.contains("Public development tracker"));
        assertTrue(html.contains("Tavall PvP"));
        assertTrue(html.contains("Staff mode"));
        assertTrue(html.contains("75%"));
    }
}
