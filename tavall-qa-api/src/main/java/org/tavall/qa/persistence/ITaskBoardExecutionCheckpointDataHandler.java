package org.tavall.qa.persistence;

import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Atomic lease/checkpoint/audit policy through the mapped Tavall Database entity contract. */
public interface ITaskBoardExecutionCheckpointDataHandler {
    TaskBoardExecutionLeaseData acquireLease(TaskBoardExecutionLeaseRequest request, UUID actorAccountId, Instant now);
    TaskBoardExecutionLeaseData renewLease(TaskBoardExecutionLeaseData expected, Duration duration, UUID actorAccountId, Instant now);
    TaskBoardExecutionLeaseData releaseLease(TaskBoardExecutionLeaseData expected, UUID actorAccountId, Instant now);
    TaskBoardExecutionCheckpointData appendCheckpoint(TaskBoardExecutionCheckpointRequest request, UUID actorAccountId, Instant now);
    Optional<TaskBoardExecutionLeaseData> currentLease(UUID taskId);
    List<TaskBoardExecutionCheckpointData> checkpoints(UUID taskId, UUID runId, int limit);
    Optional<TaskBoardExecutionCheckpointPayloadData> loadPayload(UUID taskId, UUID checkpointId);
}
