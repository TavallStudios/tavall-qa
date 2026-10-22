package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TaskBoardComment(
        UUID commentId,
        UUID taskId,
        UUID parentCommentId,
        UUID authorMinecraftUUID,
        TaskBoardCommentType type,
        String body,
        Instant createdAt,
        Instant updatedAt
) {
    public TaskBoardComment {
        Objects.requireNonNull(commentId, "commentId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(type, "type");
        body = requireText(body, "body");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
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
