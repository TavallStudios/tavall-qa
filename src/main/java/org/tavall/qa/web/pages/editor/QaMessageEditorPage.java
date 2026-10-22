package org.tavall.qa.web.pages.editor;

import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.web.frontend.design.ForgeAnimations;

/**
 * Message Editor page implementing Desktop Direction A and Mobile Direction C.
 */
public final class QaMessageEditorPage extends AbstractPage<QaMessageEditorPage> {

    public static QaMessageEditorPage build(
            String messageKey,
            String locale,
            String platform,
            String delivery,
            String content,
            String returnTaskUrl
    ) {
        return new QaMessageEditorPage()
                .title("Message Editor — " + messageKey + " | Tavall QA")
                .html(QaMessageEditorPageHTML.build(messageKey, locale, platform, delivery, content, returnTaskUrl))
                .animations(ForgeAnimations.reveal());
    }
}
