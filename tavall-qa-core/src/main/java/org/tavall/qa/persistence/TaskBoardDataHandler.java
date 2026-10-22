package org.tavall.qa.persistence;

import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.persistence.entity.TaskBoardActionEntity;
import org.tavall.qa.persistence.entity.TaskBoardCommentEntity;
import org.tavall.qa.persistence.entity.TaskBoardProjectEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TaskBoardDataHandler implements ITaskBoardDataHandler {
    private final IPostgresDatabase database;

    public TaskBoardDataHandler(IPostgresDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public boolean isAvailable() {
        return database.isAvailable() && database.entities().isOpen();
    }

    @Override
    public List<TaskBoardProject> findProjects() {
        requireAvailable();
        return database.entities().findNamed(
                        TaskBoardProjectEntity.class,
                        TaskBoardProjectEntity.FIND_ALL,
                        Map.of(),
                        0
                ).stream()
                .map(TaskBoardProjectEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardProject> findProject(String projectKey) {
        requireAvailable();
        return database.entities()
                .find(TaskBoardProjectEntity.class, requireText(projectKey, "projectKey"))
                .map(TaskBoardProjectEntity::toDomain);
    }

    @Override
    public TaskBoardProject saveProject(TaskBoardProject project) {
        requireAvailable();
        TaskBoardProjectEntity saved = database.entities().save(
                TaskBoardProjectEntity.fromDomain(Objects.requireNonNull(project, "project"))
        );
        return saved.toDomain();
    }

    @Override
    public List<TaskBoardTask> findTasks(String projectKey) {
        requireAvailable();
        String safeProjectKey = requireText(projectKey, "projectKey");
        return database.entities().findNamed(
                        TaskBoardTaskEntity.class,
                        TaskBoardTaskEntity.FIND_BY_PROJECT,
                        Map.of("projectKey", safeProjectKey),
                        0
                ).stream()
                .map(TaskBoardTaskEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<TaskBoardTask> findTask(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities()
                .find(TaskBoardTaskEntity.class, taskId.toString())
                .map(TaskBoardTaskEntity::toDomain);
    }

    @Override
    public TaskBoardTask saveTask(TaskBoardTask task) {
        requireAvailable();
        TaskBoardTaskEntity saved = database.entities().save(
                TaskBoardTaskEntity.fromDomain(Objects.requireNonNull(task, "task"))
        );
        return saved.toDomain();
    }

    @Override
    public List<TaskBoardAction> findActions(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardActionEntity.class,
                        TaskBoardActionEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardActionEntity::toDomain)
                .toList();
    }

    @Override
    public TaskBoardAction saveAction(TaskBoardAction action) {
        requireAvailable();
        TaskBoardActionEntity saved = database.entities().save(
                TaskBoardActionEntity.fromDomain(Objects.requireNonNull(action, "action"))
        );
        return saved.toDomain();
    }

    @Override
    public List<TaskBoardComment> findComments(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(
                        TaskBoardCommentEntity.class,
                        TaskBoardCommentEntity.FIND_BY_TASK,
                        Map.of("taskId", taskId.toString()),
                        0
                ).stream()
                .map(TaskBoardCommentEntity::toDomain)
                .toList();
    }

    @Override
    public TaskBoardComment saveComment(TaskBoardComment comment) {
        requireAvailable();
        TaskBoardCommentEntity saved = database.entities().save(
                TaskBoardCommentEntity.fromDomain(Objects.requireNonNull(comment, "comment"))
        );
        return saved.toDomain();
    }

    private void requireAvailable() {
        if (!isAvailable()) {
            throw new IllegalStateException("Task Board PostgreSQL storage is unavailable.");
        }
    }

    private String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
