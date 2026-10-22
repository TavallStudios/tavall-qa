package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITaskBoardDataHandler {
    boolean isAvailable();

    List<TaskBoardProject> findProjects();

    Optional<TaskBoardProject> findProject(String projectKey);

    TaskBoardProject saveProject(TaskBoardProject project);

    List<TaskBoardTask> findTasks(String projectKey);

    Optional<TaskBoardTask> findTask(UUID taskId);

    TaskBoardTask saveTask(TaskBoardTask task);

    List<TaskBoardAction> findActions(UUID taskId);

    TaskBoardAction saveAction(TaskBoardAction action);

    List<TaskBoardComment> findComments(UUID taskId);

    TaskBoardComment saveComment(TaskBoardComment comment);
}
