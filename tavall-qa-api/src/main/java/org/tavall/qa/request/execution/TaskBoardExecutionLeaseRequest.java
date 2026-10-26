package org.tavall.qa.request.execution;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/** Stable caller-generated run/lease identities and the task revision it inspected. */
public record TaskBoardExecutionLeaseRequest(UUID taskId, UUID runId, UUID leaseId,
        String expectedTaskFingerprint, Duration duration) {
    public static final Duration MAX_DURATION = Duration.ofMinutes(15);

    public TaskBoardExecutionLeaseRequest {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(expectedTaskFingerprint, "expectedTaskFingerprint");
        validateDuration(duration);
        if (!expectedTaskFingerprint.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Lease requires an exact task fingerprint");
        }
    }

    public static void validateDuration(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero() || duration.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException("Lease duration must be positive and within fifteen minutes");
        }
    }
}
