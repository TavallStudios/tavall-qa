package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskBoardAction(
        UUID actionId,
        UUID taskId,
        TaskBoardActionType type,
        String label,
        String target,
        int sortOrder,
        Instant createdAt
) {
    public TaskBoardAction {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(type, "type");
        label = requireText(label, "label");
        target = requireText(target, "target");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return normalized;
    }
}
