package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TaskBoardAuditEntry(
        UUID eventId,
        Optional<UUID> taskId,
        String projectKey,
        Optional<UUID> actorAccountId,
        UUID actorMinecraftUuid,
        TaskBoardAuditEventType eventType,
        String summary,
        Instant occurredAt
) {
    public TaskBoardAuditEntry {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        projectKey = projectKey.trim();
        if (projectKey.isBlank()) {
            throw new IllegalArgumentException("projectKey must not be blank");
        }
        Objects.requireNonNull(actorAccountId, "actorAccountId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(summary, "summary");
        summary = summary.trim();
        if (summary.isBlank()) {
            throw new IllegalArgumentException("Audit summary must not be blank");
        }
        Objects.requireNonNull(occurredAt, "occurredAt");
    }
}
