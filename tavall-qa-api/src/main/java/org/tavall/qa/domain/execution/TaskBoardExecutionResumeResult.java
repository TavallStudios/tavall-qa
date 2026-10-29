package org.tavall.qa.domain.execution;

import java.util.Objects;

/** Verified current task/source checkpoint and a bounded lease; this still grants no model or host capability. */
public record TaskBoardExecutionResumeResult(TaskBoardExecutionLeaseData lease,
        TaskBoardExecutionCheckpointData checkpoint, TaskBoardExecutionCheckpointPayloadData payload) {
    public TaskBoardExecutionResumeResult {
        Objects.requireNonNull(lease, "lease");
        Objects.requireNonNull(checkpoint, "checkpoint");
        Objects.requireNonNull(payload, "payload");
        if (!lease.taskId().equals(checkpoint.taskId()) || !lease.runId().equals(checkpoint.runId())
                || lease.checkpointSequence() != checkpoint.sequence()
                || !lease.taskFingerprint().equals(checkpoint.taskFingerprint())) {
            throw new IllegalArgumentException("Resume context must describe one current task/run sequence");
        }
    }
}
