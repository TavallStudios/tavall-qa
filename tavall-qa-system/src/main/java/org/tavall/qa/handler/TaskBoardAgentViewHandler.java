package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAction;
import org.tavall.qa.domain.TaskBoardAssignment;
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
import org.tavall.qa.view.TaskBoardAgentTaskContextView;
import org.tavall.qa.view.TaskBoardAgentTaskView;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of agent-facing role-aware task views.
 * Allows agents to read authorized tasks, obtain bounded task-specific context without a workspace dump,
 * and update execution/evidence through owner APIs.
 */
@Component
@DelegatesTo(ITaskBoardAgentViewHandler.class)
public final class TaskBoardAgentViewHandler implements ITaskBoardAgentViewHandler {
    private final ITaskBoardQueryHandler queryHandler;
    private final ITaskBoardMutationHandler mutationHandler;
    private final ITaskBoardDataHandler dataHandler;
    private final ITaskBoardNotionSyncHandler syncHandler;

    public TaskBoardAgentViewHandler(
            ITaskBoardQueryHandler queryHandler,
            ITaskBoardMutationHandler mutationHandler,
            ITaskBoardDataHandler dataHandler,
            ITaskBoardNotionSyncHandler syncHandler
    ) {
        this.queryHandler = Objects.requireNonNull(queryHandler, "queryHandler");
        this.mutationHandler = Objects.requireNonNull(mutationHandler, "mutationHandler");
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.syncHandler = Objects.requireNonNull(syncHandler, "syncHandler");
    }

    @Override
    public List<TaskBoardAgentTaskView> findAuthorizedAgentTasks(QaSubjectContext subject, Optional<String> projectKey) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(projectKey, "projectKey");

        if (!subject.hasQaServiceContext()) {
            return List.of();
        }

        List<TaskBoardProject> projects;
        if (projectKey.isPresent()) {
            Optional<TaskBoardProject> project = dataHandler.findProject(projectKey.get());
            projects = project.map(List::of).orElse(List.of());
        } else {
            projects = dataHandler.findProjects();
        }

        List<TaskBoardAgentTaskView> result = new ArrayList<>();
        for (TaskBoardProject project : projects) {
            List<TaskBoardTask> tasks = dataHandler.findTasks(project.projectKey());
            for (TaskBoardTask task : tasks) {
                boolean authorized = subject.canViewTask(project, task)
                        || subject.isAssignedToTask(task.taskId())
                        || subject.canManageTasks()
                        || subject.hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION);

                if (authorized) {
                    Optional<NotionTaskSyncState> syncState = syncHandler.findSyncStateByTaskId(subject, task.taskId());
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

        Optional<TaskBoardTask> taskOpt = dataHandler.findTask(taskId);
        if (taskOpt.isEmpty()) {
            return Optional.empty();
        }
        TaskBoardTask task = taskOpt.get();
        Optional<TaskBoardProject> projectOpt = dataHandler.findProject(task.projectKey());
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

        List<TaskBoardAction> actions = dataHandler.findActions(taskId);
        List<TaskBoardComment> comments = dataHandler.findComments(taskId);
        List<TaskBoardEvidence> evidence = dataHandler.findEvidenceForTask(taskId);
        List<TaskBoardAssignment> assignments = dataHandler.findAssignments(taskId);
        Optional<NotionTaskSyncState> syncState = syncHandler.findSyncStateByTaskId(subject, taskId);

        Map<String, String> planningContext = new HashMap<>();
        planningContext.put("taskId", taskId.toString());
        planningContext.put("projectKey", task.projectKey());
        planningContext.put("qaRevision", String.valueOf(task.qaRevision()));
        planningContext.put("sourceType", task.sourceType());
        planningContext.put("sourceReference", task.sourceReference());

        syncState.ifPresent(state -> {
            planningContext.put("notionPageId", state.notionPageId());
            planningContext.put("notionPageUrl", state.notionPageUrl());
            planningContext.put("notionDatabaseId", state.notionDatabaseId());
            planningContext.put("lastIngestedRevision", String.valueOf(state.lastIngestedRevision()));
            planningContext.put("hasConflict", String.valueOf(state.hasConflict()));
            if (state.hasConflict()) {
                planningContext.put("conflictDetails", state.conflictDetails());
            }
        });

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

        TaskBoardTask current = dataHandler.findTask(request.taskId())
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + request.taskId()));

        TaskBoardTaskStatus targetStatus = request.targetStatus() != null ? request.targetStatus() : current.status();
        if (Boolean.TRUE.equals(request.blocked())) {
            targetStatus = TaskBoardTaskStatus.BLOCKED;
        }

        TaskBoardTask updated = current;
        if (targetStatus != current.status()) {
            updated = mutationHandler.updateTask(subject, new TaskBoardTaskUpdateRequest(
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
        }

        if (!request.executionNote().isBlank()) {
            mutationHandler.addComment(subject, new TaskBoardCommentCreateRequest(
                    current.taskId(),
                    null,
                    TaskBoardCommentType.QA_NOTE,
                    request.executionNote(),
                    org.tavall.qa.domain.auth.QaEnvironmentGrant.DEV
            ));
        }

        if (syncHandler instanceof TaskBoardNotionSyncHandler notionHandler) {
            notionHandler.advanceOutboundRevision(request.taskId());
        }

        return updated;
    }
}
