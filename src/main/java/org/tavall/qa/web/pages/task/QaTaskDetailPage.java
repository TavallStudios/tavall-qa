package org.tavall.qa.web.pages.task;

import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.web.frontend.abstracts.page.AbstractPage;
import org.tavall.qa.web.pages.shared.QaPageAnimations;

/**
 * Direction A Task Detail page at {@code qa.tavall.org/projects/{projectKey}/tasks/{taskId}}.
 *
 * <p>Preserves evidence-first layout, NHV badge state, human verification audit trail,
 * Direction A contextual action rail, and task execution diagnostics.</p>
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
        QaTaskExecutionView execution = context != null ? context.execution().orElse(null) : null;
        return build(taskView, canManage, canHumanVerify, context, execution);
    }

    public static QaTaskDetailPage build(
            TaskBoardTaskView taskView,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext context,
            QaTaskExecutionView execution
    ) {
        String title = taskView != null && taskView.task() != null
                ? taskView.task().title() + " | " + taskView.project().displayName() + " | Tavall QA"
                : "Task Detail | Tavall QA";

        return new QaTaskDetailPage()
                .title(title)
                .html(QaTaskDetailPageHTML.build(taskView, canManage, canHumanVerify, context, execution))
                .animations(QaPageAnimations.build());
    }

    public static QaTaskDetailPage build(TaskBoardTaskPageView taskPage, boolean canManage, boolean canHumanVerify) {
        return build(taskPage, canManage, canHumanVerify, QaTaskPageContext.readOnly(), null);
    }

    public static QaTaskDetailPage build(TaskBoardTaskPageView taskPage, QaTaskPageContext context) {
        QaTaskExecutionView execution = context != null ? context.execution().orElse(null) : null;
        return build(taskPage, false, false, context, execution);
    }

    public static QaTaskDetailPage build(
            TaskBoardTaskPageView taskPage,
            boolean canManage,
            boolean canHumanVerify,
            QaTaskPageContext context,
            QaTaskExecutionView execution
    ) {
        String title = taskPage != null
                ? taskPage.title() + " | " + taskPage.projectDisplayName() + " | Tavall QA"
                : "Task Detail | Tavall QA";

        return new QaTaskDetailPage()
                .title(title)
                .html(QaTaskDetailPageHTML.build(taskPage, canManage, canHumanVerify, context, execution))
                .animations(QaPageAnimations.build());
    }
}
