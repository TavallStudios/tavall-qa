package org.tavall.qa.request.execution;

import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;

import java.util.Objects;
import java.util.UUID;

/** Appends one observation against the exact lease revision returned by QA. */
public record TaskBoardExecutionCheckpointRequest(UUID checkpointId, TaskBoardExecutionLeaseData expectedLease,
        TaskBoardExecutionSourceData source, String payloadSha256, String providerId, TaskBoardExecutionStatus status) {
    public TaskBoardExecutionCheckpointRequest {
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(expectedLease, "expectedLease");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(payloadSha256, "payloadSha256");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(status, "status");
        if (!payloadSha256.matches("[a-f0-9]{64}") || !providerId.matches("[a-z0-9][a-z0-9._-]{0,95}")) {
            throw new IllegalArgumentException("Payload and provider identities must be exact");
        }
    }
}
