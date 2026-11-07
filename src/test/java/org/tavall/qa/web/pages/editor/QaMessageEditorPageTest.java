package org.tavall.qa.web.pages.editor;

import org.junit.jupiter.api.Test;
import org.tavall.web.frontend.render.HTMLRenderer;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct house-style test for {@link QaMessageEditorPage}: the real page composes the real
 * {@code QaMessageEditorPageHTML} builder. No collaborators are faked.
 */
final class QaMessageEditorPageTest {

    private final HTMLRenderer renderer = new HTMLRenderer();

    @Test
    void rendersDesktopDirectionAAndMobileDirectionC() {
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
