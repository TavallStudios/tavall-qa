package org.tavall.qa.handler;

import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardCommentType;
import org.tavall.qa.domain.TaskBoardEvidence;
import org.tavall.qa.domain.TaskBoardProject;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.notion.NotionTaskSyncState;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardAgentExecutionUpdateRequest;
import org.tavall.qa.request.TaskBoardCommentCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.view.TaskBoardAgentPlanningContext;
import org.tavall.qa.view.TaskBoardAgentTaskContextView;
import org.tavall.qa.view.TaskBoardAgentTaskView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of agent-facing role-aware task views.
 * Allows agents to read authorized tasks, obtain bounded task-specific context without a workspace dump,
 * and update execution/evidence through owner APIs.
 */
@DelegatesTo(ITaskBoardAgentViewHandler.class)
public final class TaskBoardAgentViewHandler extends GenerationOwnedDependencyAccess
        implements ITaskBoardAgentViewHandler,
        DependencyAccess<ITaskBoardMutationHandler, ITaskBoardDataHandler, ITaskBoardNotionSyncHandler> {

    @Override
    public List<TaskBoardAgentTaskView> findAuthorizedAgentTasks(QaSubjectContext subject, Optional<String> projectKey) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(projectKey, "projectKey");

        if (!subject.hasQaServiceContext()) {
            return List.of();
        }

        List<TaskBoardProject> projects;
        if (projectKey.isPresent()) {
            Optional<TaskBoardProject> project = getInstance().taskBoardDataHandler().findProject(projectKey.get());
            projects = project.map(List::of).orElse(List.of());
        } else {
            projects = getInstance().taskBoardDataHandler().findProjects();
        }

        List<TaskBoardAgentTaskView> result = new ArrayList<>();
        for (TaskBoardProject project : projects) {
            List<TaskBoardTask> tasks = getInstance().taskBoardDataHandler().findTasks(project.projectKey());
            for (TaskBoardTask task : tasks) {
                boolean authorized = subject.canViewTask(project, task)
                        || subject.isAssignedToTask(task.taskId())
                        || subject.canManageTasks()
                        || subject.hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION);

                if (authorized) {
                    Optional<NotionTaskSyncState> syncState = getInstance().taskBoardNotionSyncHandler().findSyncStateByTaskId(subject, task.taskId());
                    String notionUrl = syncState.map(NotionTaskSyncState::notionPageUrl).orElse("");
                    long syncRev = syncState.map(NotionTaskSyncState::lastOutboundRevision).orElse(0L);
                    boolean assigned = subject.isAssignedToTask(task.taskId());

                    result.add(new TaskBoardAgentTaskView(
                            task.taskId(),
                            task.projectKey(),
                            task.sectionKey(),
                            task.title(),
                            task.status(),
                            task.priority(),
                            task.qaStatus(),
                            task.audience(),
                            assigned,
                            notionUrl,
                            syncRev,
                            task.updatedAt()
                    ));
                }
            }
        }
        return List.copyOf(result);
    }

    @Override
    public Optional<TaskBoardAgentTaskContextView> findAgentTaskContext(QaSubjectContext subject, UUID taskId) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(taskId, "taskId");

        if (!subject.hasQaServiceContext()) {
            return Optional.empty();
        }

        Optional<TaskBoardTask> taskOpt = getInstance().taskBoardDataHandler().findTask(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }
        TaskBoardTask task = taskOpt.get();
        Optional<TaskBoardProject> projectOpt = getInstance().taskBoardDataHandler().findProject(task.projectKey());
        if (projectOpt.isEmpty()) {
            return Optional.empty();
        }
        TaskBoardProject project = projectOpt.get();

        boolean authorized = subject.canViewTask(project, task)
                || subject.isAssignedToTask(task.taskId())
                || subject.canManageTasks()
                || subject.hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION);

        if (!authorized) {
            return Optional.empty();
        }

        List<TaskBoardAction> actions = getInstance().taskBoardDataHandler().findActions(taskId);
        List<TaskBoardComment> comments = getInstance().taskBoardDataHandler().findComments(taskId);
        List<TaskBoardEvidence> evidence = getInstance().taskBoardDataHandler().findEvidenceForTask(taskId);
        List<TaskBoardAssignment> assignments = getInstance().taskBoardDataHandler().findAssignments(taskId);
        Optional<NotionTaskSyncState> syncState = getInstance().taskBoardNotionSyncHandler().findSyncStateByTaskId(subject, taskId);

        TaskBoardAgentPlanningContext planningContext = new TaskBoardAgentPlanningContext(
                taskId,
                task.projectKey(),
                task.qaRevision(),
                task.sourceType(),
                task.sourceReference(),
                TaskBoardTaskFingerprint.compute(task));

        return Optional.of(new TaskBoardAgentTaskContextView(
                task,
                syncState,
                actions,
                comments,
                evidence,
                assignments,
                planningContext
        ));
    }

    @Override
    public TaskBoardTask updateAgentExecution(QaSubjectContext subject, TaskBoardAgentExecutionUpdateRequest request) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(request, "request");

        if (!subject.hasQaServiceContext()) {
            throw new SecurityException("Unauthorized: active QA service context required");
        }

        boolean authorized = subject.canManageTasks()
                || subject.isAssignedToTask(request.taskId())
                || subject.hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION);

        if (!authorized) {
            throw new SecurityException("Unauthorized: caller not authorized to update execution on task " + request.taskId());
        }

        TaskBoardTask current = getInstance().taskBoardDataHandler().findTask(request.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.taskId()));

        TaskBoardTaskStatus targetStatus = request.targetStatus() != null ? request.targetStatus() : current.status();
        if (Boolean.TRUE.equals(request.blocked())) {
            targetStatus = TaskBoardTaskStatus.BLOCKED;
        }

        TaskBoardTask updated = current;
        if (targetStatus != current.status()) {
            if (subject.canManageTasks()) {
                updated = getInstance().taskBoardMutationHandler().updateTask(subject, new TaskBoardTaskUpdateRequest(
                        current.taskId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        targetStatus,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
            } else {
                Instant now = Instant.now();
                updated = new TaskBoardTask(
                        current.taskId(),
                        current.projectKey(),
                        current.sectionKey(),
                        current.title(),
                        current.publicTitle(),
                        current.description(),
                        current.publicSummary(),
                        targetStatus,
                        current.priority(),
                        current.qaStatus(),
                        current.audience(),
                        current.weight(),
                        current.hostValidationRequired(),
                        current.sourceType(),
                        current.sourceReference(),
                        current.qaQcEligible(),
                        current.qaRevision(),
                        current.createdByAccountId(),
                        current.createdByMinecraftUUID(),
                        current.verifiedByAccountId(),
                        current.verifiedByMinecraftUUID(),
                        current.verifiedEvidenceId(),
                        current.humanVerifiedAt(),
                        current.sortOrder(),
                        current.createdAt(),
                        now
                );
                TaskBoardAuditEntry audit = new TaskBoardAuditEntry(
                        UUID.randomUUID(),
                        Optional.of(current.taskId()),
                        current.projectKey(),
                        subject.accountId(),
                        subject.linkedMinecraftUuid().orElse(null),
                        org.tavall.qa.domain.TaskBoardAuditEventType.TASK_UPDATED,
                        "Agent execution updated status to " + targetStatus,
                        now
                );
                getInstance().taskBoardDataHandler().saveTask(updated, Optional.of(current.updatedAt()), audit);
            }
        }

        if (!request.executionNote().isBlank()) {
            Instant now = Instant.now();
            TaskBoardComment comment = new TaskBoardComment(
                    UUID.randomUUID(),
                    current.taskId(),
                    null,
                    subject.accountId().orElseThrow(),
                    subject.linkedMinecraftUuid().orElse(null),
                    TaskBoardCommentType.COMMENT,
                    request.executionNote(),
                    now,
                    now
            );
            TaskBoardAuditEntry audit = new TaskBoardAuditEntry(
                    UUID.randomUUID(),
                    Optional.of(current.taskId()),
                    current.projectKey(),
                    subject.accountId(),
                    subject.linkedMinecraftUuid().orElse(null),
                    org.tavall.qa.domain.TaskBoardAuditEventType.COMMENT_ADDED,
                    "Agent added execution note",
                    now
            );
            getInstance().taskBoardDataHandler().saveComment(comment, audit);
        }

        if (getInstance().taskBoardNotionSyncHandler() instanceof TaskBoardNotionSyncHandler notionHandler) {
            notionHandler.advanceOutboundRevision(request.taskId());
        }

        return updated;
    }
}
