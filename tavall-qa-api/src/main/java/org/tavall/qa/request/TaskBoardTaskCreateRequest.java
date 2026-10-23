package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardTaskPriority;

import java.util.UUID;

public record TaskBoardTaskCreateRequest(
        String projectKey,
        String sectionKey,
        String title,
        String publicTitle,
        String description,
        String publicSummary,
        TaskBoardTaskPriority priority,
        TaskBoardAudience audience,
        int weight,
        boolean hostValidationRequired,
        boolean qaQcEligible,
        String sourceType,
        String sourceReference,
        int sortOrder
) {
}
