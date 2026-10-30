package org.tavall.qa.domain.notion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Durable per-task synchronization state linking a Notion page identity to a QA task identity.
 * Survives process restarts via PostgreSQL authority and NFS file-backed redundancy.
 */
public record NotionTaskSyncState(
        UUID syncId,
        UUID taskId,
        String notionDatabaseId,
        String notionPageId,
        String notionPageUrl,
        Instant lastIngestedRevision,
        long lastOutboundRevision,
        long lastAcknowledgedOutboundRevision,
        boolean hasConflict,
        String conflictDetails,
        boolean archived,
        boolean deleted,
        Instant createdAt,
        Instant updatedAt
) {
    public NotionTaskSyncState {
        Objects.requireNonNull(syncId, "syncId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(notionDatabaseId, "notionDatabaseId");
        Objects.requireNonNull(notionPageId, "notionPageId");
        Objects.requireNonNull(notionPageUrl, "notionPageUrl");
        conflictDetails = conflictDetails == null ? "" : conflictDetails.trim();
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public boolean hasPendingOutboundChanges() {
        return lastOutboundRevision > lastAcknowledgedOutboundRevision;
    }

    public NotionTaskSyncState withIngestedRevision(Instant revision, boolean archived, boolean deleted, Instant now) {
        return new NotionTaskSyncState(
                syncId,
                taskId,
                notionDatabaseId,
                notionPageId,
                notionPageUrl,
                revision,
                lastOutboundRevision,
                lastAcknowledgedOutboundRevision,
                hasConflict,
                conflictDetails,
                archived,
                deleted,
                createdAt,
                now
        );
    }

    public NotionTaskSyncState withConflict(String details, Instant now) {
        return new NotionTaskSyncState(
                syncId,
                taskId,
                notionDatabaseId,
                notionPageId,
                notionPageUrl,
                lastIngestedRevision,
                lastOutboundRevision,
                lastAcknowledgedOutboundRevision,
                true,
                details,
                archived,
                deleted,
                createdAt,
                now
        );
    }

    public NotionTaskSyncState withAdvancedOutboundRevision(long newOutboundRevision, Instant now) {
        return new NotionTaskSyncState(
                syncId,
                taskId,
                notionDatabaseId,
                notionPageId,
                notionPageUrl,
                lastIngestedRevision,
                Math.max(lastOutboundRevision + 1, newOutboundRevision),
                lastAcknowledgedOutboundRevision,
                hasConflict,
                conflictDetails,
                archived,
                deleted,
                createdAt,
                now
        );
    }

    public NotionTaskSyncState withAcknowledgedOutboundRevision(long ackRevision, Instant now) {
        boolean resolvedConflict = hasConflict && ackRevision >= lastOutboundRevision;
        return new NotionTaskSyncState(
                syncId,
                taskId,
                notionDatabaseId,
                notionPageId,
                notionPageUrl,
                lastIngestedRevision,
                lastOutboundRevision,
                Math.max(lastAcknowledgedOutboundRevision, ackRevision),
                resolvedConflict ? false : hasConflict,
                resolvedConflict ? "" : conflictDetails,
                archived,
                deleted,
                createdAt,
                now
        );
    }
}
