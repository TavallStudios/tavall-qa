package org.tavall.qa.persistence;

import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.qa.domain.TaskBoardVenture;
import org.tavall.qa.persistence.entity.TaskBoardVentureEntity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TaskBoardVentureDataHandler implements ITaskBoardVentureDataHandler {
    private final IPostgresDatabase database;

    public TaskBoardVentureDataHandler(IPostgresDatabase database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    @Override
    public List<TaskBoardVenture> findVentures() {
        requireAvailable();
        return database.entities().findNamed(TaskBoardVentureEntity.class,
                        TaskBoardVentureEntity.FIND_ALL, Map.of(), 0)
                .stream().map(TaskBoardVentureEntity::toDomain).toList();
    }

    @Override
    public Optional<TaskBoardVenture> findVenture(String slug) {
        requireAvailable();
        String key = Objects.requireNonNull(slug, "slug").trim();
        if (key.isEmpty()) {
            throw new IllegalArgumentException("slug must not be blank");
        }
        return database.entities().find(TaskBoardVentureEntity.class, key)
                .map(TaskBoardVentureEntity::toDomain);
    }

    private void requireAvailable() {
        if (!database.isAvailable() || !database.entities().isOpen()) {
            throw new org.tavall.qa.domain.TaskBoardStorageUnavailableException(
                    "Task Board PostgreSQL storage is unavailable.");
        }
    }
}
