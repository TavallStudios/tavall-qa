package org.tavall.qa.request.execution;

import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;

import java.util.Objects;
import java.util.UUID;

/** Appends one observation against the exact lease revision returned by QA. */
public record TaskBoardExecutionCheckpointRequest(UUID checkpointId, TaskBoardExecutionLeaseData expectedLease,
        TaskBoardExecutionSourceData source, TaskBoardExecutionCheckpointPayloadData payload,
        String providerId, TaskBoardExecutionStatus status) {
    public TaskBoardExecutionCheckpointRequest {
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(expectedLease, "expectedLease");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(status, "status");
        if (!source.diffSha256().equals(payload.diff().sha256()) || !providerId.matches("[a-z0-9][a-z0-9._-]{0,95}")) {
            throw new IllegalArgumentException("Diff and provider identities must be exact");
        }
    }
}
