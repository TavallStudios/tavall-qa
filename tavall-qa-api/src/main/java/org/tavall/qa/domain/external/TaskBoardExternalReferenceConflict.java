package org.tavall.qa.domain.external;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Durable conflict queue entry with normalized metadata only; provider payload bodies are not copied. */
public record TaskBoardExternalReferenceConflict(
        UUID conflictId,
        TaskBoardExternalReferenceKey key,
        Optional<UUID> referenceId,
        Optional<UUID> existingTaskId,
        UUID requestedTaskId,
        String incomingDisplayKey,
        String incomingSourceUrl,
        TaskBoardExternalReferenceSourceStatus incomingSourceStatus,
        String incomingSourceRevision,
        String incomingSourceCursor,
        String incomingSourceFingerprint,
        Instant incomingObservedAt,
        long expectedSyncRevision,
        String reason,
        TaskBoardExternalReferenceConflictStatus status,
        Optional<TaskBoardExternalReferenceConflictResolution> resolution,
        Optional<UUID> resolvedByAccountId,
        Instant createdAt,
        Optional<Instant> resolvedAt
) {
    public TaskBoardExternalReferenceConflict {
        Objects.requireNonNull(conflictId, "conflictId");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(existingTaskId, "existingTaskId");
        Objects.requireNonNull(requestedTaskId, "requestedTaskId");
        incomingDisplayKey = requireText(incomingDisplayKey, "incomingDisplayKey", 255);
        incomingSourceUrl = requireText(incomingSourceUrl, "incomingSourceUrl", 2048);
        Objects.requireNonNull(incomingSourceStatus, "incomingSourceStatus");
        incomingSourceRevision = requireText(incomingSourceRevision, "incomingSourceRevision", 255);
        incomingSourceCursor = normalize(incomingSourceCursor, 512);
        incomingSourceFingerprint = requireText(incomingSourceFingerprint, "incomingSourceFingerprint", 128);
        Objects.requireNonNull(incomingObservedAt, "incomingObservedAt");
        if (expectedSyncRevision < 0) throw new IllegalArgumentException("expectedSyncRevision must not be negative");
        reason = requireText(reason, "reason", 2048);
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(resolution, "resolution");
        Objects.requireNonNull(resolvedByAccountId, "resolvedByAccountId");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(resolvedAt, "resolvedAt");
        boolean hasResolution = resolution.isPresent() && resolvedAt.isPresent() && resolvedByAccountId.isPresent();
        boolean hasNoResolution = resolution.isEmpty() && resolvedAt.isEmpty() && resolvedByAccountId.isEmpty();
        if (status == TaskBoardExternalReferenceConflictStatus.RESOLVED ? !hasResolution : !hasNoResolution) {
            throw new IllegalArgumentException("Conflict resolution fields must match the conflict status");
        }
    }

    private static String requireText(String value, String name, int maxLength) {
        Objects.requireNonNull(value, name);
        String trimmed = value.trim();
        if (trimmed.isBlank() || trimmed.length() > maxLength) {
            throw new IllegalArgumentException(name + " must contain 1.." + maxLength + " characters");
        }
        return trimmed;
    }

    private static String normalize(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("External reference value is too long");
        return normalized;
    }
}
