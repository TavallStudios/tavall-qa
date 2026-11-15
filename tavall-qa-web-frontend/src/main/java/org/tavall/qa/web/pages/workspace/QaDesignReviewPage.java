package org.tavall.qa.web.pages.workspace;

import org.tavall.qa.view.TaskBoardTaskView;
import java.util.List;
import org.tavall.web.frontend.abstracts.page.AbstractPage;

/** Live review page; authorization and request identity are supplied by the host. */
public final class QaDesignReviewPage extends AbstractPage<QaDesignReviewPage> {
    public static QaDesignReviewPage build(TaskBoardTaskView task, QaWorkspacePageContext context, boolean canSelect,
                                          List<TaskBoardTaskView> openReviews) {
        return new QaDesignReviewPage().title("Design review: " + task.task().title() + " | Tavall QA")
                .html(QaDesignReviewPageHTML.build(task, context, canSelect, openReviews))
                .animations(org.tavall.qa.web.pages.shared.QaPageAnimations.build());
    }
}
