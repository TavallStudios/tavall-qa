package org.tavall.qa.domain.notion;

import java.time.Instant;
import java.util.Objects;

/**
 * Acknowledgement sent by an authorized agent after successfully writing an outbound change to Notion.
 */
public record NotionTaskSyncAckRequest(
        String notionPageId,
        long syncRevision,
        Instant acknowledgedAt
) {
    public NotionTaskSyncAckRequest {
        Objects.requireNonNull(notionPageId, "notionPageId");
        if (syncRevision < 0) {
            throw new IllegalArgumentException("syncRevision must not be negative");
        }
        acknowledgedAt = acknowledgedAt == null ? Instant.now() : acknowledgedAt;
    }
}
