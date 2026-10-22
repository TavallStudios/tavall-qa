package org.tavall.qa.view;

import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.UUID;

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
        Instant updatedAt
) {
}
