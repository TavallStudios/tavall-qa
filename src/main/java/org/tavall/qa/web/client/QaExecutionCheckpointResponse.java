package org.tavall.qa.web.client;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representation of a task execution checkpoint returned by the QA execution backend
 * (GET /api/execution/tasks/{taskId}/checkpoints).
 */
public record QaExecutionCheckpointResponse(
        UUID checkpointId,
        UUID taskId,
        UUID runId,
        UUID leaseId,
        long sequence,
        String taskFingerprint,
        String payloadSha256,
        String providerId,
        String status,
        UUID recordedByAccountId,
        Instant recordedAt
) {
    public QaExecutionCheckpointResponse {
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(recordedAt, "recordedAt");
    }
}
