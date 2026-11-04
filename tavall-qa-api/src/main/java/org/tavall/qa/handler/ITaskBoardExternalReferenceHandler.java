package org.tavall.qa.handler;

import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Privileged owner API for exact external-reference queries, mutations, and conflict review. */
public interface ITaskBoardExternalReferenceHandler {
    TaskBoardExternalReferenceMutationResult registerOrReconcile(
            QaSubjectContext subject,
            TaskBoardExternalReferenceMutationRequest request
    );

    Optional<TaskBoardExternalReference> findReference(
            QaSubjectContext subject,
            TaskBoardExternalReferenceKey key
    );

    List<TaskBoardExternalReference> findReferencesForTask(QaSubjectContext subject, UUID taskId);

    List<TaskBoardExternalReferenceConflict> findOpenConflicts(QaSubjectContext subject, Optional<UUID> taskId);

    TaskBoardExternalReferenceConflict resolveConflict(
            QaSubjectContext subject,
            TaskBoardExternalReferenceConflictResolutionRequest request
    );
}
