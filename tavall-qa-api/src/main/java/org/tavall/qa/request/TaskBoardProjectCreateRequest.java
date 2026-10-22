package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectStatus;

public record TaskBoardProjectCreateRequest(
        String projectKey,
        String displayName,
        String description,
        TaskBoardProjectStatus status,
        TaskBoardAudience audience,
        int sortOrder
) {
}
