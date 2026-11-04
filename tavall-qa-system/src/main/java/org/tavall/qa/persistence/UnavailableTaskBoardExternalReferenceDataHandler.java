package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Typed unavailable-state implementation used when mapped QA storage is not ready. */
public final class UnavailableTaskBoardExternalReferenceDataHandler implements ITaskBoardExternalReferenceDataHandler {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<TaskBoardExternalReference> findReference(TaskBoardExternalReferenceKey key) {
        return Optional.empty();
    }

    @Override
    public List<TaskBoardExternalReference> findReferencesForTask(UUID taskId) {
        return List.of();
    }

    @Override
    public List<TaskBoardExternalReferenceConflict> findOpenConflicts(Optional<UUID> taskId) {
        return List.of();
    }

    @Override
    public Optional<TaskBoardExternalReferenceConflict> findConflict(UUID conflictId) {
        return Optional.empty();
    }

    @Override
    public TaskBoardExternalReferenceMutationResult registerOrReconcile(
            TaskBoardExternalReferenceMutationRequest request,
            TaskBoardAuditEntry auditEntry
    ) {
        throw unavailable();
    }

    @Override
    public TaskBoardExternalReferenceConflict resolveConflict(
            TaskBoardExternalReferenceConflictResolutionRequest request,
            UUID actorAccountId,
            TaskBoardAuditEntry auditEntry
    ) {
        throw unavailable();
    }

    private static TaskBoardStorageUnavailableException unavailable() {
        return new TaskBoardStorageUnavailableException("QA Task Board storage is unavailable");
    }
}
