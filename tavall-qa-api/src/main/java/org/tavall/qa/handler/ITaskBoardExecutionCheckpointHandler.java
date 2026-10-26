package org.tavall.qa.handler;

import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Private execution context: every operation requires current Account and DEV task authority. */
public interface ITaskBoardExecutionCheckpointHandler {
    TaskBoardExecutionLeaseData acquireLease(QaSubjectContext subject, TaskBoardExecutionLeaseRequest request);
    TaskBoardExecutionLeaseData renewLease(QaSubjectContext subject, TaskBoardExecutionLeaseData expected, Duration duration);
    TaskBoardExecutionLeaseData releaseLease(QaSubjectContext subject, TaskBoardExecutionLeaseData expected);
    TaskBoardExecutionCheckpointData appendCheckpoint(QaSubjectContext subject, TaskBoardExecutionCheckpointRequest request);
    Optional<TaskBoardExecutionLeaseData> currentLease(QaSubjectContext subject, UUID taskId);
    List<TaskBoardExecutionCheckpointData> checkpoints(QaSubjectContext subject, UUID taskId, UUID runId, int limit);
}
