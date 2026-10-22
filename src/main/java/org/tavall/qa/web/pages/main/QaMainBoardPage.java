package org.tavall.qa.web.pages.main;

import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.web.frontend.design.ForgeAnimations;

/**
 * Public QA & Task Board main landing page at {@code qa.tavall.org/}.
 *
 * <p>Renders accepted Direction C synthesis: project-only index, weighted overall progress,
 * and compact status metrics.</p>
 */
public final class QaMainBoardPage extends AbstractPage<QaMainBoardPage> {

    public static QaMainBoardPage build(TaskBoardPublicDashboardView dashboard, boolean canManage, boolean canHumanVerify, String identityStatus) {
        return new QaMainBoardPage()
                .title("Tavall QA — Follow what changed")
                .html(QaMainBoardPageHTML.build(dashboard, canManage, canHumanVerify, identityStatus))
                .animations(ForgeAnimations.reveal());
    }
}
