package org.tavall.qa.domain;

import java.time.Instant;
import java.util.Objects;

public record TaskBoardProject(
        String projectKey,
        String displayName,
        String description,
        TaskBoardProjectStatus status,
        TaskBoardAudience audience,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public TaskBoardProject {
        projectKey = requireText(projectKey, "projectKey");
        displayName = requireText(displayName, "displayName");
        description = description == null ? "" : description.trim();
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(audience, "audience");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public boolean publicVisible() {
        return audience == TaskBoardAudience.PUBLIC
                && status != TaskBoardProjectStatus.ARCHIVED;
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
