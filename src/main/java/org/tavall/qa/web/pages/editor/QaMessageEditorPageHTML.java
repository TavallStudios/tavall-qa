package org.tavall.qa.web.pages.editor;

import org.tavall.qa.web.generated.QaCSS;
import org.tavall.qa.web.pages.shared.QaProduct;
import org.tavall.qa.web.routes.QaRoutes;
import org.tavall.web.api.html.HTMLAttribute;
import org.tavall.web.api.html.HTMLDocument;
import org.tavall.web.api.html.HTMLElement;
import org.tavall.web.frontend.design.Forge;
import org.tavall.web.frontend.generated.TavallForgeCSS;
import org.tavall.web.frontend.html.HTML;

import java.util.List;

/**
 * Message Editor layout:
 * Desktop Direction A: Preview dock, Minecraft preview card, copy editor.
 * Mobile Direction C: Focus tabs (PREVIEW / EDIT / MESSAGES), fixed bottom action bar.
 */
public final class QaMessageEditorPageHTML {

    private QaMessageEditorPageHTML() {
    }

    public static HTMLDocument build(
            String messageKey,
            String locale,
            String platform,
            String delivery,
            String content,
            String returnTaskUrl
    ) {
        String key = messageKey != null ? messageKey : "ffa.match.countdown";
        String loc = locale != null ? locale : "en_us";
        String plat = platform != null ? platform : "UNIVERSAL";
        String del = delivery != null ? delivery : "TITLE";
        String text = content != null ? content : "<gold>Match starts in {{countdown.seconds}}</gold>\n<gray>Kit: {{kit.name}} · tavall.org</gray>";

        HTMLElement shell = HTML.div().classes(
                QaCSS.QA_APP,
                QaCSS.PUBLIC_TASK_BOARD_PAGE,
                QaCSS.PUBLIC_BOARD_MAIN,
                TavallForgeCSS.FORGE_REVEAL
        );

        // Hero
        HTMLElement hero = HTML.section().classes(QaCSS.CONTENT_ADMIN_HERO, QaCSS.MESSAGE_EDITOR_HERO);
        HTMLElement heroCopy = HTML.div();
        heroCopy.child(HTML.p().classes(QaCSS.EYEBROW).text("Tavall · Message System"));
        heroCopy.child(HTML.h1().text("Edit Message"));
        heroCopy.child(HTML.p().text("Edit canonical game copy, inspect its runtime identity, and preview player-facing rendering."));
        hero.child(heroCopy);

        HTMLElement runtimeStrip = HTML.div().classes(QaCSS.MESSAGE_EDITOR_RUNTIME);
        runtimeStrip.child(HTML.span().text("Postgres message_config"));
        runtimeStrip.child(HTML.span().text("Postgres + Redis live update"));
        hero.child(runtimeStrip);
        shell.child(hero);

        // Mobile C Tabs (PREVIEW / EDIT / MESSAGES)
        HTMLElement mobileTabs = HTML.div().classes(QaCSS.MESSAGE_MOBILE_TABS);
        mobileTabs.child(HTML.button().classes(QaCSS.MESSAGE_MOBILE_TAB_BTN, QaCSS.ACTIVE)
                .attribute(HTMLAttribute.TYPE, "button").text("PREVIEW"));
        mobileTabs.child(HTML.button().classes(QaCSS.MESSAGE_MOBILE_TAB_BTN)
                .attribute(HTMLAttribute.TYPE, "button").text("EDIT"));
        mobileTabs.child(HTML.button().classes(QaCSS.MESSAGE_MOBILE_TAB_BTN)
                .attribute(HTMLAttribute.TYPE, "button").text("MESSAGES"));
        shell.child(mobileTabs);

        // Workbench (Desktop A layout: Left Navigator, Right Editor + Preview Dock)
        HTMLElement workbench = HTML.div().classes(QaCSS.MESSAGE_WORKBENCH);

        // Left: Navigator
        HTMLElement nav = HTML.aside().classes(QaCSS.MESSAGE_NAVIGATOR);
        nav.child(HTML.h3().text("Game Messages"));
        HTMLElement navList = HTML.div().classes(QaCSS.MESSAGE_NAVIGATOR_LIST);
        for (String msg : List.of("ffa.match.countdown", "ffa.match.start", "ffa.match.win", "ffa.player.eliminated", "core.welcome")) {
            HTMLElement item = HTML.a().classes(QaCSS.MESSAGE_NAVIGATOR_ITEM);
            if (msg.equals(key)) {
                item.classes(QaCSS.ACTIVE);
            }
            item.text(msg);
            navList.child(item);
        }
        nav.child(navList);
        workbench.child(nav);

        // Right: Panel with Preview & Copy Editor
        HTMLElement editorPanel = HTML.section().classes(QaCSS.MESSAGE_EDITOR_PANEL);

        // Identity strip
        HTMLElement idStrip = HTML.div().classes(QaCSS.MESSAGE_IDENTITY_STRIP);
        idStrip.child(HTML.div()
                .child(HTML.span().text("Key"))
                .child(HTML.strong().text(key)));
        idStrip.child(HTML.div()
                .child(HTML.span().text("Locale"))
                .child(HTML.strong().text(loc)));
        idStrip.child(HTML.div()
                .child(HTML.span().text("Platform"))
                .child(HTML.strong().text(plat)));
        editorPanel.child(idStrip);

        // Minecraft Preview Card (Desktop A Preview Dock)
        HTMLElement previewCard = HTML.div().classes(QaCSS.MINECRAFT_PREVIEW_CARD);
        HTMLElement previewHead = HTML.div().classes(QaCSS.MINECRAFT_PREVIEW_HEADER);
        previewHead.child(HTML.strong().text("Minecraft Preview"));
        previewHead.child(HTML.span().classes(QaCSS.MINECRAFT_PREVIEW_BADGE).text("LIVE · " + del));
        previewCard.child(previewHead);

        HTMLElement viewport = HTML.div().classes(QaCSS.MINECRAFT_PREVIEW_VIEWPORT);
        viewport.child(HTML.div().classes(QaCSS.MINECRAFT_CROSSHAIR));

        HTMLElement titleWrap = HTML.div().classes(QaCSS.MINECRAFT_TITLE_WRAP);
        titleWrap.child(HTML.div().classes(QaCSS.MINECRAFT_TITLE_TEXT).text("Match starts in 5"));
        titleWrap.child(HTML.div().classes(QaCSS.MINECRAFT_SUBTITLE_TEXT).text("Kit: Beast · tavall.org"));
        viewport.child(titleWrap);
        previewCard.child(viewport);
        editorPanel.child(previewCard);

        // Copy Editor Form
        HTMLElement form = HTML.form().classes(QaCSS.PUBLIC_STAFF_FORM);
        form.child(HTML.label().classes(QaCSS.WIDE)
                .child(HTML.span().text("Message Copy (" + text.length() + " characters)"))
                .child(HTML.textarea().text(text)));

        form.child(HTML.label()
                .child(HTML.span().text("Delivery Surface"))
                .child(HTML.select().child(HTML.option().text(del))));

        form.child(HTML.label()
                .child(HTML.span().text("Formatting"))
                .child(HTML.select().child(HTML.option().text("MINI_MESSAGE"))));

        // Actions: Save & Return
        HTMLElement actions = HTML.div().classes(QaCSS.MESSAGE_EDITOR_ACTIONS);
        actions.child(HTML.button().classes(QaCSS.GHOST_BUTTON)
                .attribute(HTMLAttribute.TYPE, "button")
                .text("Save"));
        actions.child(HTML.a().classes(QaCSS.GOLD_BUTTON)
                .attribute(HTMLAttribute.HREF, returnTaskUrl != null ? returnTaskUrl : QaRoutes.ROOT)
                .text("Save & Return to Task"));
        form.child(actions);
        editorPanel.child(form);

        workbench.child(editorPanel);
        shell.child(workbench);

        return Forge.page(QaProduct.product(QaRoutes.ROOT), shell);
    }
}
