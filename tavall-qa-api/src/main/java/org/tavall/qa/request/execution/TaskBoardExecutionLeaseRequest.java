package org.tavall.qa.request.execution;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/** Stable caller-generated run/lease identities and the task revision it inspected. */
public record TaskBoardExecutionLeaseRequest(UUID taskId, UUID runId, UUID leaseId,
        String expectedTaskFingerprint, Duration duration) {
    public TaskBoardExecutionLeaseRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(expectedTaskFingerprint, "expectedTaskFingerprint");
        Objects.requireNonNull(duration, "duration");
        if (!expectedTaskFingerprint.matches("[a-f0-9]{64}") || duration.isNegative() || duration.isZero()
                || duration.compareTo(Duration.ofMinutes(15)) > 0) {
            throw new IllegalArgumentException("Lease requires a task fingerprint and duration within fifteen minutes");
        }
    }
}
