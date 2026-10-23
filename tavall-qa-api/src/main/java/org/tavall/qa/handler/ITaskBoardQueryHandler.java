package org.tavall.qa.handler;

import org.tavall.qa.domain.auth.QaTaskAssignmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.view.TaskBoardDashboardView;
import org.tavall.qa.view.TaskBoardProjectView;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.qa.view.TaskBoardTaskView;
import org.tavall.qa.view.TaskBoardTaskPageView;
import org.tavall.qa.view.TaskBoardQaProgressionView;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface ITaskBoardQueryHandler {
    TaskBoardDashboardView dashboard(QaSubjectContext subject);

    TaskBoardPublicDashboardView publicDashboard();

    Optional<TaskBoardProjectView> findProjectView(String projectKey, QaSubjectContext subject);

    Optional<TaskBoardPublicProjectView> findPublicProjectView(String projectKey);

    Optional<TaskBoardPublicTaskView> findPublicTaskView(UUID taskId);

    Optional<TaskBoardTaskView> findTaskView(UUID taskId, QaSubjectContext subject);

    List<QaTaskAssignmentGrant> findActiveAssignments(UUID accountId);

    Optional<TaskBoardTaskPageView> findAssignedTaskPageView(UUID taskId, QaSubjectContext subject);

    List<TaskBoardTaskPageView> findAssignedTaskPages(QaSubjectContext subject);

    Optional<TaskBoardQaProgressionView> findQaProgression(UUID accountId, QaSubjectContext subject);
}
