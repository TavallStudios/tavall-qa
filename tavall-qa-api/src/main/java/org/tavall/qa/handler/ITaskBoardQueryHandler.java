package org.tavall.qa.handler;

import org.tavall.qa.view.TaskBoardDashboardView;
import org.tavall.qa.view.TaskBoardProjectView;
import org.tavall.qa.view.TaskBoardPublicDashboardView;
import org.tavall.qa.view.TaskBoardPublicProjectView;
import org.tavall.qa.view.TaskBoardPublicTaskView;
import org.tavall.qa.view.TaskBoardTaskView;

import java.util.Optional;
import java.util.UUID;

public interface ITaskBoardQueryHandler {
    TaskBoardDashboardView dashboard();

    TaskBoardPublicDashboardView publicDashboard();

    Optional<TaskBoardProjectView> findProjectView(String projectKey);

    Optional<TaskBoardPublicProjectView> findPublicProjectView(String projectKey);

    Optional<TaskBoardPublicTaskView> findPublicTaskView(UUID taskId);

    Optional<TaskBoardTaskView> findTaskView(UUID taskId);
}
