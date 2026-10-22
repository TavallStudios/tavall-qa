package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardActionType;

import java.util.UUID;

public record TaskBoardActionCreateRequest(
        UUID taskId,
        TaskBoardActionType type,
        String label,
        String target,
        int sortOrder
) {
}
