package org.tavall.qa.domain.execution;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** One task's current bounded work lease; revisions fence renewal and checkpoint writers. */
public record TaskBoardExecutionLeaseData(UUID taskId, UUID runId, UUID leaseId, UUID ownerAccountId,
        long revision, long checkpointSequence, String taskFingerprint, Instant expiresAt) {
    public TaskBoardExecutionLeaseData {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(ownerAccountId, "ownerAccountId");
        Objects.requireNonNull(taskFingerprint, "taskFingerprint");
        Objects.requireNonNull(expiresAt, "expiresAt");
        if (revision < 0 || checkpointSequence < 0 || !taskFingerprint.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Lease revisions and task fingerprint must be valid");
        }
    }

    public boolean activeAt(Instant instant) {
        return expiresAt.isAfter(Objects.requireNonNull(instant, "instant"));
    }
}
