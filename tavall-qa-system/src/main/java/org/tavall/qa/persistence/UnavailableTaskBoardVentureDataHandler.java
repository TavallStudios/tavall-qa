package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardVenture;

import java.util.List;
import java.util.Optional;

/** Fails visibly when the venture directory's durable authority is unavailable. */
public final class UnavailableTaskBoardVentureDataHandler implements ITaskBoardVentureDataHandler {
    @Override
    public List<TaskBoardVenture> findVentures() {
        throw unavailable();
    }

    @Override
    public Optional<TaskBoardVenture> findVenture(String slug) {
        throw unavailable();
    }

    private static TaskBoardStorageUnavailableException unavailable() {
        return new TaskBoardStorageUnavailableException("Task Board PostgreSQL storage is unavailable.");
    }
}
