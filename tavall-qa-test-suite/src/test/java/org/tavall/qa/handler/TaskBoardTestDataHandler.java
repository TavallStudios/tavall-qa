package org.tavall.qa.handler;

import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.persistence.ITaskBoardDataHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class TaskBoardTestDataHandler implements ITaskBoardDataHandler {
    private final Map<String, TaskBoardProject> projects = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardTask> tasks = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardAction> actions = new LinkedHashMap<>();
    private final Map<UUID, TaskBoardComment> comments = new LinkedHashMap<>();

    @Override public boolean isAvailable() { return true; }
    @Override public List<TaskBoardProject> findProjects() { return List.copyOf(projects.values()); }
    @Override public Optional<TaskBoardProject> findProject(String projectKey) { return Optional.ofNullable(projects.get(projectKey)); }
    @Override public TaskBoardProject saveProject(TaskBoardProject project) { projects.put(project.projectKey(), project); return project; }
    @Override public List<TaskBoardTask> findTasks(String projectKey) { return tasks.values().stream().filter(task -> task.projectKey().equals(projectKey)).toList(); }
    @Override public Optional<TaskBoardTask> findTask(UUID taskId) { return Optional.ofNullable(tasks.get(taskId)); }
    @Override public TaskBoardTask saveTask(TaskBoardTask task) { tasks.put(task.taskId(), task); return task; }
    @Override public List<TaskBoardAction> findActions(UUID taskId) { return actions.values().stream().filter(action -> action.taskId().equals(taskId)).toList(); }
    @Override public TaskBoardAction saveAction(TaskBoardAction action) { actions.put(action.actionId(), action); return action; }
    @Override public List<TaskBoardComment> findComments(UUID taskId) { return comments.values().stream().filter(comment -> comment.taskId().equals(taskId)).toList(); }
    @Override public TaskBoardComment saveComment(TaskBoardComment comment) { comments.put(comment.commentId(), comment); return comment; }
}
