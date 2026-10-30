package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agent-facing role-aware view of an authorized task.
 */
public record TaskBoardAgentTaskView(
        UUID taskId,
        String projectKey,
        String sectionKey,
        String title,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        TaskBoardAudience audience,
        boolean assignedToCaller,
        String notionPageUrl,
        long syncRevision,
        Instant updatedAt
) {
    public TaskBoardAgentTaskView {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(projectKey, "projectKey");
        Objects.requireNonNull(sectionKey, "sectionKey");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(qaStatus, "qaStatus");
        Objects.requireNonNull(audience, "audience");
        notionPageUrl = notionPageUrl == null ? "" : notionPageUrl.trim();
        Objects.requireNonNull(updatedAt, "updatedAt");
    }
}
