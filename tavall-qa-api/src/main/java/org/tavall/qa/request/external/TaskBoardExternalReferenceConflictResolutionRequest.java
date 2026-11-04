package org.tavall.qa.request.external;

import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictResolution;

import java.util.Objects;
import java.util.UUID;

public record TaskBoardExternalReferenceConflictResolutionRequest(
        UUID conflictId,
        TaskBoardExternalReferenceConflictResolution resolution,
        long expectedSyncRevision,
        String resolutionNote
) {
    public TaskBoardExternalReferenceConflictResolutionRequest {
        Objects.requireNonNull(conflictId, "conflictId");
        Objects.requireNonNull(resolution, "resolution");
        if (expectedSyncRevision < 0) throw new IllegalArgumentException("expectedSyncRevision must not be negative");
        resolutionNote = resolutionNote == null ? "" : resolutionNote.trim();
        if (resolutionNote.length() > 2048) throw new IllegalArgumentException("resolutionNote is too long");
        if (resolution == TaskBoardExternalReferenceConflictResolution.APPLY_INCOMING && resolutionNote.isBlank()) {
            throw new IllegalArgumentException("Applying an incoming reference requires an operator resolution note");
        }
    }
}
