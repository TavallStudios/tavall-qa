package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class UnavailableTaskBoardDataHandler implements ITaskBoardDataHandler {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<TaskBoardProject> findProjects() {
        return List.of();
    }

    @Override
    public Optional<TaskBoardProject> findProject(String projectKey) {
        return Optional.empty();
    }

    @Override
    public TaskBoardProject saveProject(TaskBoardProject project) {
        throw new IllegalStateException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardTask> findTasks(String projectKey) {
        return List.of();
    }

    @Override
    public Optional<TaskBoardTask> findTask(UUID taskId) {
        return Optional.empty();
    }

    @Override
    public TaskBoardTask saveTask(TaskBoardTask task) {
        throw new IllegalStateException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardAction> findActions(UUID taskId) {
        return List.of();
    }

    @Override
    public TaskBoardAction saveAction(TaskBoardAction action) {
        throw new IllegalStateException("Task Board PostgreSQL storage is unavailable.");
    }

    @Override
    public List<TaskBoardComment> findComments(UUID taskId) {
        return List.of();
    }

    @Override
    public TaskBoardComment saveComment(TaskBoardComment comment) {
        throw new IllegalStateException("Task Board PostgreSQL storage is unavailable.");
    }
}
