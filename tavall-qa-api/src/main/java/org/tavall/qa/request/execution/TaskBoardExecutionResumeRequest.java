package org.tavall.qa.request.execution;

import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;

import java.util.Objects;
import java.util.UUID;

/** The authorized execution host supplies its current source observation, not provider conversation history. */
public record TaskBoardExecutionResumeRequest(TaskBoardExecutionLeaseRequest leaseRequest, UUID checkpointId,
        TaskBoardExecutionSourceData observedSource) {
    public TaskBoardExecutionResumeRequest {
        Objects.requireNonNull(leaseRequest, "leaseRequest");
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(observedSource, "observedSource");
    }
}
