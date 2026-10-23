package org.tavall.qa.web.pages.project;

import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.qa.web.pages.shared.QaPageAnimations;

/**
 * Direction A Project Detail page at {@code qa.tavall.org/projects/{projectKey}}.
 *
 * <p>Preserves system/section hierarchy with compact Visible, Current, Done, NHV metrics,
 * 3-task initial visibility with 4-task progressive expansion.</p>
 */
public final class QaProjectDetailPage extends AbstractPage<QaProjectDetailPage> {

    public static QaProjectDetailPage build(
            TaskBoardPublicProjectView projectView,
            boolean canManage,
            boolean canHumanVerify,
            int nhvDebtCount,
            int blockedCount
    ) {
        return build(projectView, canManage, canHumanVerify, nhvDebtCount, blockedCount, QaProjectPageContext.readOnly());
    }

    public static QaProjectDetailPage build(
            TaskBoardPublicProjectView projectView,
            boolean canManage,
            boolean canHumanVerify,
            int nhvDebtCount,
            int blockedCount,
            QaProjectPageContext context
    ) {
        String title = projectView != null && projectView.summary() != null
                ? projectView.summary().displayName() + " Task Board | Tavall QA"
                : "Project Task Board | Tavall QA";

        return new QaProjectDetailPage()
                .title(title)
                .html(QaProjectDetailPageHTML.build(projectView, canManage, canHumanVerify, nhvDebtCount, blockedCount, context))
                .animations(QaPageAnimations.build());
    }
}
