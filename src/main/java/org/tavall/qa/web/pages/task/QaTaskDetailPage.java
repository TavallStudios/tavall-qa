package org.tavall.qa.web.pages.task;

import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.web.frontend.design.ForgeAnimations;

/**
 * Direction A Task Detail page at {@code qa.tavall.org/projects/{projectKey}/tasks/{taskId}}.
 *
 * <p>Preserves evidence-first layout, NHV badge state, human verification audit trail,
 * and Direction A contextual action rail.</p>
 */
public final class QaTaskDetailPage extends AbstractPage<QaTaskDetailPage> {

    public static QaTaskDetailPage build(TaskBoardTaskView taskView, boolean canManage, boolean canHumanVerify) {
        return build(taskView, canManage, canHumanVerify, QaTaskPageContext.readOnly());
    }

    public static QaTaskDetailPage build(
            TaskBoardTaskView taskView,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext context
    ) {
        String title = taskView != null && taskView.task() != null
                ? taskView.task().title() + " | " + taskView.project().displayName() + " | Tavall QA"
                : "Task Detail | Tavall QA";

        return new QaTaskDetailPage()
                .title(title)
                .html(QaTaskDetailPageHTML.build(taskView, canManage, canHumanVerify, context))
                .animations(ForgeAnimations.reveal());
    }

    public static QaTaskDetailPage build(TaskBoardTaskPageView taskPage, boolean canManage, boolean canHumanVerify) {
        String title = taskPage != null
                ? taskPage.title() + " | " + taskPage.projectDisplayName() + " | Tavall QA"
                : "Task Detail | Tavall QA";

        return new QaTaskDetailPage()
                .title(title)
                .html(QaTaskDetailPageHTML.build(taskPage, canManage, canHumanVerify))
                .animations(ForgeAnimations.reveal());
    }

    public static QaTaskDetailPage build(TaskBoardTaskPageView taskPage, QaTaskPageContext context) {
        String title = taskPage != null
                ? taskPage.title() + " | " + taskPage.projectDisplayName() + " | Tavall QA"
                : "Task Detail | Tavall QA";

        return new QaTaskDetailPage()
                .title(title)
                .html(QaTaskDetailPageHTML.build(taskPage, false, false, context))
                .animations(ForgeAnimations.reveal());
    }
}
