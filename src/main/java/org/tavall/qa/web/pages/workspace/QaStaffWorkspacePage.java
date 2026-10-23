package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.qa.web.pages.shared.QaPageAnimations;

/** Direction C Focus Workspace at {@code qa.tavall.org/workspace/{projectKey}/tasks/{taskId}}. */
public final class QaStaffWorkspacePage extends AbstractPage<QaStaffWorkspacePage> {

    public static QaStaffWorkspacePage build(TaskBoardTaskView taskView, QaWorkspacePageContext context) {
        String title = taskView != null && taskView.task() != null
                ? "Focus Workspace: " + taskView.task().title() + " | Tavall QA"
                : "Staff Focus Workspace | Tavall QA";

        return new QaStaffWorkspacePage()
                .title(title)
                .html(QaStaffWorkspacePageHTML.build(taskView, context))
                .animations(QaPageAnimations.build());
    }
}
