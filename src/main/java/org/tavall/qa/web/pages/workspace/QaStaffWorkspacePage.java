package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.web.frontend.design.ForgeAnimations;

/**
 * Direction C Focus Workspace at {@code qa.tavall.org/workspace/{projectKey}/tasks/{taskId}}.
 *
 * <p>Preserves accepted Focus Workspace interaction model: Overview / QA / Edit / Thread modes,
 * URL-hash focus restoration, soft controls, and non-mutating visual state matrix preview.</p>
 */
public final class QaStaffWorkspacePage extends AbstractPage<QaStaffWorkspacePage> {

    public static QaStaffWorkspacePage build(
            TaskBoardTaskView taskView,
            String activeFocus,
            String statePreview,
            boolean canHumanVerify,
            String verifierUuid
    ) {
        String title = taskView != null && taskView.task() != null
                ? "Focus Workspace: " + taskView.task().title() + " | Tavall QA"
                : "Staff Focus Workspace | Tavall QA";

        return new QaStaffWorkspacePage()
                .title(title)
                .html(QaStaffWorkspacePageHTML.build(taskView, activeFocus, statePreview, canHumanVerify, verifierUuid))
                .animations(ForgeAnimations.reveal());
    }

    public static QaStaffWorkspacePage build(
            TaskBoardTaskPageView taskPage,
            String activeFocus,
            String statePreview,
            boolean canHumanVerify,
            String verifierUuid
    ) {
        String title = taskPage != null
                ? "Focus Workspace: " + taskPage.title() + " | Tavall QA"
                : "Staff Focus Workspace | Tavall QA";

        return new QaStaffWorkspacePage()
                .title(title)
                .html(QaStaffWorkspacePageHTML.build(taskPage, activeFocus, statePreview, canHumanVerify, verifierUuid))
                .animations(ForgeAnimations.reveal());
    }
}
