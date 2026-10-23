package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.Objects;

public record TaskBoardPublicTaskView(
        UUID taskId,
        String projectKey,
        String sectionKey,
        String title,
        String summary,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        int weight,
        String sourceType,
        Instant updatedAt,
        List<TaskBoardPublicEvidenceView> evidence
) {
    public TaskBoardPublicTaskView {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(sectionKey, "sectionKey");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(summary, "summary");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(qaStatus, "qaStatus");
        sourceType = sourceType == null ? "" : sourceType;
        Objects.requireNonNull(updatedAt, "updatedAt");
        evidence = List.copyOf(Objects.requireNonNull(evidence, "evidence"));
    }
}
