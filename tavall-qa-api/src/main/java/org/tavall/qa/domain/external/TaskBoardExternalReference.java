package org.tavall.qa.domain.external;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** QA-owned durable link and per-source revision cursor. External fields remain a projection. */
public record TaskBoardExternalReference(
        UUID referenceId,
        UUID taskId,
        TaskBoardExternalReferenceKey key,
        String displayKey,
        String sourceUrl,
        TaskBoardExternalReferenceSourceStatus sourceStatus,
        String lastObservedRevision,
        String lastAppliedRevision,
        String lastObservedCursor,
        String sourceFingerprint,
        TaskBoardExternalReferenceSyncState syncState,
        long syncRevision,
        Instant lastObservedAt,
        Instant lastAppliedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public TaskBoardExternalReference {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(key, "key");
        displayKey = requireText(displayKey, "displayKey", 255);
        sourceUrl = requireText(sourceUrl, "sourceUrl", 2048);
        Objects.requireNonNull(sourceStatus, "sourceStatus");
        lastObservedRevision = normalize(lastObservedRevision, 255);
        lastAppliedRevision = normalize(lastAppliedRevision, 255);
        lastObservedCursor = normalize(lastObservedCursor, 512);
        sourceFingerprint = normalize(sourceFingerprint, 128);
        Objects.requireNonNull(syncState, "syncState");
        if (syncRevision < 1) throw new IllegalArgumentException("syncRevision must be positive");
        Objects.requireNonNull(lastObservedAt, "lastObservedAt");
        Objects.requireNonNull(lastAppliedAt, "lastAppliedAt");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
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
