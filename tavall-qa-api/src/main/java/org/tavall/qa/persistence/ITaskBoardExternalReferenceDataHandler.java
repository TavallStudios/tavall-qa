package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Mapped-JPA persistence contract for owner-scoped external task references and conflict history. */
public interface ITaskBoardExternalReferenceDataHandler {
    boolean isAvailable();

    Optional<TaskBoardExternalReference> findReference(TaskBoardExternalReferenceKey key);

    List<TaskBoardExternalReference> findReferencesForTask(UUID taskId);

    List<TaskBoardExternalReferenceConflict> findOpenConflicts(Optional<UUID> taskId);

    Optional<TaskBoardExternalReferenceConflict> findConflict(UUID conflictId);

    TaskBoardExternalReferenceMutationResult registerOrReconcile(
            TaskBoardExternalReferenceMutationRequest request,
            TaskBoardAuditEntry auditEntry
    );

    TaskBoardExternalReferenceConflict resolveConflict(
            TaskBoardExternalReferenceConflictResolutionRequest request,
            UUID actorAccountId,
            TaskBoardAuditEntry auditEntry
    );
}
