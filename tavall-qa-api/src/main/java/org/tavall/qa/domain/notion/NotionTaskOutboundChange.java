package org.tavall.qa.domain.notion;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed outbound status and context change to be exported and written to Notion by an authorized agent.
 */
public record NotionTaskOutboundChange(
        UUID syncId,
        UUID taskId,
        String notionDatabaseId,
        String notionPageId,
        String notionPageUrl,
        long syncRevision,
        String status,
        boolean blocked,
        String blockerSummary,
        boolean evidenceComplete,
        String currentStep,
        Instant lastReconciled,
        Instant generatedAt
) {
    public NotionTaskOutboundChange {
        Objects.requireNonNull(syncId, "syncId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(notionDatabaseId, "notionDatabaseId");
        Objects.requireNonNull(notionPageId, "notionPageId");
        Objects.requireNonNull(notionPageUrl, "notionPageUrl");
        status = status == null ? "Not started" : status.trim();
        blockerSummary = blockerSummary == null ? "" : blockerSummary.trim();
        currentStep = currentStep == null ? "" : currentStep.trim();
        Objects.requireNonNull(generatedAt, "generatedAt");
    }
}
