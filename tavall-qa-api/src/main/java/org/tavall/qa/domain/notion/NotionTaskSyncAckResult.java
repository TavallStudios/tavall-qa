package org.tavall.qa.domain.notion;

import java.util.Objects;

/**
 * Result of an outbound sync acknowledgement. Stale or duplicate acknowledgements are safe no-ops.
 */
public record NotionTaskSyncAckResult(
        String notionPageId,
        long acknowledgedRevision,
        boolean staleOrDuplicate,
        String message
) {
    public NotionTaskSyncAckResult {
        Objects.requireNonNull(notionPageId, "notionPageId");
        message = message == null ? "" : message.trim();
    }
}
