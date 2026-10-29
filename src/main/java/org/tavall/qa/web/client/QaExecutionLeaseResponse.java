package org.tavall.qa.web.client;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Representation of a task execution lease returned by the QA execution backend
 * (GET /api/execution/tasks/{taskId}/lease).
 */
public record QaExecutionLeaseResponse(
        UUID taskId,
        UUID runId,
        UUID leaseId,
        UUID ownerAccountId,
        long revision,
        long checkpointSequence,
        String taskFingerprint,
        Instant expiresAt
) {
    public QaExecutionLeaseResponse {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(ownerAccountId, "ownerAccountId");
        Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public boolean isActive(Instant now) {
        return expiresAt.isAfter(Objects.requireNonNull(now, "now"));
    }
}
