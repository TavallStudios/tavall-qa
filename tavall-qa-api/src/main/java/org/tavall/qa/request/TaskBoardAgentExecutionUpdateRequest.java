package org.tavall.qa.request;

import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.util.Objects;
import java.util.UUID;

/**
 * Agent request to update task execution state or evidence notes through owner APIs.
 */
public record TaskBoardAgentExecutionUpdateRequest(
        UUID taskId,
        TaskBoardTaskStatus targetStatus,
        Boolean blocked,
        String blockerReason,
        String executionNote
) {
    public TaskBoardAgentExecutionUpdateRequest {
        Objects.requireNonNull(taskId, "taskId");
        blockerReason = blockerReason == null ? "" : blockerReason.trim();
        executionNote = executionNote == null ? "" : executionNote.trim();
    }
}
