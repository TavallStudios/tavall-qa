package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.UUID;

public record TaskBoardTaskUpdateRequest(
        UUID taskId,
        String sectionKey,
        String title,
        String publicTitle,
        String description,
        String publicSummary,
        TaskBoardTaskStatus status,
        TaskBoardTaskPriority priority,
        TaskBoardQaStatus qaStatus,
        TaskBoardAudience audience,
        int weight,
        boolean hostValidationRequired,
        String sourceType,
        String sourceReference,
        UUID actorMinecraftUUID,
        int sortOrder
) {
}
