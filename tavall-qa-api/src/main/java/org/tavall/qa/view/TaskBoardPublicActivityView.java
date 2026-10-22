package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskBoardPublicActivityView(
        UUID taskId,
        String projectKey,
        String projectDisplayName,
        String sectionKey,
        String title,
        String summary,
        TaskBoardTaskStatus status,
        TaskBoardQaStatus qaStatus,
        Instant updatedAt
) {
}
