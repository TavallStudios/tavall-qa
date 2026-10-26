package org.tavall.qa.domain.execution;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable task-owned envelope for a typed, content-addressed continuation payload. */
public record TaskBoardExecutionCheckpointData(UUID checkpointId, UUID taskId, UUID runId, UUID leaseId,
        long sequence, String taskFingerprint, TaskBoardExecutionSourceData source,
        String payloadSha256, String providerId, TaskBoardExecutionStatus status,
        UUID recordedByAccountId, Instant recordedAt) {
    public TaskBoardExecutionCheckpointData {
        Objects.requireNonNull(checkpointId, "checkpointId");
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(runId, "runId");
        Objects.requireNonNull(leaseId, "leaseId");
        Objects.requireNonNull(taskFingerprint, "taskFingerprint");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(payloadSha256, "payloadSha256");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(recordedByAccountId, "recordedByAccountId");
        Objects.requireNonNull(recordedAt, "recordedAt");
        if (sequence < 1 || !taskFingerprint.matches("[a-f0-9]{64}") || !payloadSha256.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Checkpoint sequence and digests must be valid");
        }
        if (!providerId.matches("[a-z0-9][a-z0-9._-]{0,95}")) {
            throw new IllegalArgumentException("providerId must be a normalized provider identity");
        }
    }

    public URI payloadReference() {
        return URI.create("tavall-storage://artifacts/sha256/" + payloadSha256);
    }
}
