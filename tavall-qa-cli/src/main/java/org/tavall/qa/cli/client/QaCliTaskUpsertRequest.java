package org.tavall.qa.cli.client;

import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/** Typed upsert request for the QA execution task board. */
public record QaCliTaskUpsertRequest(
        String externalKey,
        String title,
        TaskBoardTaskStatus status,
        String description,
        TaskBoardTaskPriority priority,
        String section,
        String project,
        String sourceType,
        String sourceReference,
        Path credentialFile) {

    public QaCliTaskUpsertRequest {
        externalKey = requireText(externalKey, "externalKey");
        title = requireText(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        section = requireText(section, "section");
        project = requireText(project, "project");
        sourceType = requireText(sourceType, "sourceType");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is blank");
        }
        return normalized;
    }
}
