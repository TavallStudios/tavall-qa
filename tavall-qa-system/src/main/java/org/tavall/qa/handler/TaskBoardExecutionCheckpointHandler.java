package org.tavall.qa.handler;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionResumeRequest;
import org.tavall.qa.domain.execution.TaskBoardExecutionResumeResult;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Current Account authority is checked for every private checkpoint read and mutation. */
@DelegatesTo(ITaskBoardExecutionCheckpointHandler.class)
public final class TaskBoardExecutionCheckpointHandler extends GenerationOwnedDependencyAccess
        implements ITaskBoardExecutionCheckpointHandler,
        DependencyAccess<ITaskBoardExecutionCheckpointDataHandler, ITaskBoardDataHandler, Clock> {
    @Override
    public TaskBoardExecutionLeaseData acquireLease(QaSubjectContext subject, TaskBoardExecutionLeaseRequest request) {
        Objects.requireNonNull(request, "request");
        authorize(subject, request.taskId());
        TaskBoardExecutionCheckpointHandlerDependencyAccess dependencies = getInstance();
        return dependencies.taskBoardExecutionCheckpointDataHandler().acquireLease(request,
                subject.accountId().orElseThrow(), dependencies.clock().instant());
    }

    @Override
    public TaskBoardExecutionLeaseData renewLease(QaSubjectContext subject, TaskBoardExecutionLeaseData expected,
            Duration duration) {
        Objects.requireNonNull(expected, "expected");
        authorize(subject, expected.taskId());
        TaskBoardExecutionCheckpointHandlerDependencyAccess dependencies = getInstance();
        return dependencies.taskBoardExecutionCheckpointDataHandler().renewLease(expected, duration,
                subject.accountId().orElseThrow(), dependencies.clock().instant());
    }

    @Override
    public TaskBoardExecutionLeaseData releaseLease(QaSubjectContext subject, TaskBoardExecutionLeaseData expected) {
        Objects.requireNonNull(expected, "expected");
        authorize(subject, expected.taskId());
        TaskBoardExecutionCheckpointHandlerDependencyAccess dependencies = getInstance();
        return dependencies.taskBoardExecutionCheckpointDataHandler().releaseLease(expected,
                subject.accountId().orElseThrow(), dependencies.clock().instant());
    }

    @Override
    public TaskBoardExecutionCheckpointData appendCheckpoint(QaSubjectContext subject, TaskBoardExecutionCheckpointRequest request) {
        Objects.requireNonNull(request, "request");
        authorize(subject, request.expectedLease().taskId());
        TaskBoardExecutionCheckpointHandlerDependencyAccess dependencies = getInstance();
        return dependencies.taskBoardExecutionCheckpointDataHandler().appendCheckpoint(request,
                subject.accountId().orElseThrow(), dependencies.clock().instant());
    }

    @Override
    public Optional<TaskBoardExecutionLeaseData> currentLease(QaSubjectContext subject, UUID taskId) {
        authorize(subject, taskId);
        return getInstance().taskBoardExecutionCheckpointDataHandler().currentLease(taskId);
    }

    @Override
    public List<TaskBoardExecutionCheckpointData> checkpoints(QaSubjectContext subject, UUID taskId, UUID runId, int limit) {
        authorize(subject, taskId);
        return getInstance().taskBoardExecutionCheckpointDataHandler().checkpoints(taskId, runId, limit);
    }

    @Override
    public Optional<TaskBoardExecutionCheckpointPayloadData> loadPayload(QaSubjectContext subject, UUID taskId, UUID checkpointId) {
        authorize(subject, taskId);
        return getInstance().taskBoardExecutionCheckpointDataHandler().loadPayload(taskId, checkpointId);
    }

    @Override
    public TaskBoardExecutionResumeResult resumeCheckpoint(QaSubjectContext subject, TaskBoardExecutionResumeRequest request) {
        Objects.requireNonNull(request, "request");
        authorize(subject, request.leaseRequest().taskId());
        TaskBoardExecutionCheckpointHandlerDependencyAccess dependencies = getInstance();
        return dependencies.taskBoardExecutionCheckpointDataHandler().resumeCheckpoint(request,
                subject.accountId().orElseThrow(), dependencies.clock().instant());
    }

    private void authorize(QaSubjectContext subject, UUID taskId) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(taskId, "taskId");
        if (!subject.hasQaServiceContext() || !subject.hasEnvironmentGrant(QaEnvironmentGrant.DEV)
                || !subject.isGlobalQaAdministrator() && !subject.hasTechnicalDevExecution(taskId)) {
            throw new SecurityException("Current Account DEV execution authority is required for this task");
        }
        ITaskBoardDataHandler tasks = getInstance().taskBoardDataHandler();
        TaskBoardTask task = tasks.findTask(taskId).orElseThrow(() -> new IllegalArgumentException("Task is unavailable"));
        TaskBoardProject project = tasks.findProject(task.projectKey())
                .orElseThrow(() -> new IllegalArgumentException("Project is unavailable"));
        if (!subject.canViewTask(project, task)) throw new SecurityException("Task context is not visible to this Account");
    }
}
