package org.tavall.qa.handler;

import org.springframework.stereotype.Component;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.notion.NotionDatabaseTaskMapping;
import org.tavall.qa.domain.notion.NotionTaskIngestResult;
import org.tavall.qa.domain.notion.NotionTaskOutboundChange;
import org.tavall.qa.domain.notion.NotionTaskSnapshot;
import org.tavall.qa.domain.notion.NotionTaskSyncAckRequest;
import org.tavall.qa.domain.notion.NotionTaskSyncAckResult;
import org.tavall.qa.domain.notion.NotionTaskSyncState;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardNotionSyncDataHandler;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of Notion 1:1 task synchronization owner API.
 * Idempotent ingest keyed by Notion page ID with revision/conflict resolution,
 * archive/delete handling, role-aware authorization, and outbound sync export + ack.
 */
@Component
@DelegatesTo(ITaskBoardNotionSyncHandler.class)
public final class TaskBoardNotionSyncHandler implements ITaskBoardNotionSyncHandler {
    private final ITaskBoardNotionSyncDataHandler syncDataHandler;
    private final ITaskBoardDataHandler dataHandler;
    private final ITaskBoardMutationHandler mutationHandler;
    private final org.tavall.qa.domain.notion.INotionTaskMappingRegistry mappingRegistry;
    private final Clock clock;

    public TaskBoardNotionSyncHandler(
            ITaskBoardNotionSyncDataHandler syncDataHandler,
            ITaskBoardDataHandler dataHandler,
            ITaskBoardMutationHandler mutationHandler,
            org.tavall.qa.domain.notion.INotionTaskMappingRegistry mappingRegistry,
            Clock clock
    ) {
        this.syncDataHandler = Objects.requireNonNull(syncDataHandler, "syncDataHandler");
        this.dataHandler = Objects.requireNonNull(dataHandler, "dataHandler");
        this.mutationHandler = Objects.requireNonNull(mutationHandler, "mutationHandler");
        this.mappingRegistry = Objects.requireNonNull(mappingRegistry, "mappingRegistry");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TaskBoardNotionSyncHandler(
            ITaskBoardNotionSyncDataHandler syncDataHandler,
            ITaskBoardDataHandler dataHandler,
            ITaskBoardMutationHandler mutationHandler,
            Clock clock
    ) {
        this(
                syncDataHandler,
                dataHandler,
                mutationHandler,
                org.tavall.qa.domain.notion.NotionTaskMappingRegistry.canonical(),
                Objects.requireNonNull(clock, "clock")
        );
    }

    @Override
    public NotionTaskIngestResult ingestSnapshot(QaSubjectContext subject, NotionTaskSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        requireAuthorized(subject);

        Instant now = clock.instant();
        NotionDatabaseTaskMapping mapping = resolveMapping(snapshot.databaseId());
        Optional<NotionTaskSyncState> existingStateOpt = syncDataHandler.findSyncStateByPageId(snapshot.pageId());

        if (existingStateOpt.isPresent()) {
            NotionTaskSyncState existingState = existingStateOpt.get();
            TaskBoardTask currentTask = dataHandler.findTask(existingState.taskId())
                    .orElseThrow(() -> new IllegalStateException("Mapped task not found for sync state: " + existingState.taskId()));

            // Check duplicate ingest
            if (existingState.lastIngestedRevision() != null
                    && !snapshot.lastEditedTime().isAfter(existingState.lastIngestedRevision())) {
                return new NotionTaskIngestResult(
                        currentTask.taskId(),
                        snapshot.pageId(),
                        false,
                        false,
                        true,
                        false,
                        "Duplicate ingest ignored; current revision up-to-date",
                        currentTask,
                        existingState
                );
            }

            // Conflict detection: QA has unacknowledged outbound changes while Notion also advanced revision
            boolean hasPendingOutbound = existingState.hasPendingOutboundChanges();
            boolean conflict = false;
            String conflictDetails = "";

            if (hasPendingOutbound) {
                conflict = true;
                conflictDetails = "Concurrent modification: QA outbound revision " + existingState.lastOutboundRevision()
                        + " pending ack, Notion edited at " + snapshot.lastEditedTime();
            }

            // Archive or delete handling
            if (snapshot.archived() || snapshot.deleted()) {
                TaskBoardTask updatedTask = currentTask;
                if (currentTask.status() != TaskBoardTaskStatus.CANCELLED) {
                    updatedTask = mutationHandler.updateTask(subject, new TaskBoardTaskUpdateRequest(
                            currentTask.taskId(),
                            null,
                            null,
                            null,
                            null,
                            null,
                            TaskBoardTaskStatus.CANCELLED,
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
                NotionTaskSyncState updatedState = existingState.withIngestedRevision(
                        snapshot.lastEditedTime(),
                        snapshot.archived(),
                        snapshot.deleted(),
                        now
                );
                if (conflict) {
                    updatedState = updatedState.withConflict(conflictDetails, now);
                }
                syncDataHandler.saveSyncState(updatedState, audit(subject, updatedTask.taskId(), updatedTask.projectKey(),
                        "Archived/deleted Notion task synchronized", now));
                return new NotionTaskIngestResult(
                        updatedTask.taskId(),
                        snapshot.pageId(),
                        false,
                        true,
                        false,
                        conflict,
                        "Task archived/deleted from Notion",
                        updatedTask,
                        updatedState
                );
            }

            // Normal update: resolve status with conflict resolution policy
            TaskBoardTaskStatus targetStatus = mapping.mapInboundStatus(snapshot.status(), snapshot.blocked());
            if (conflict) {
                // Conflict resolution policy: If QA is already DONE or HUMAN_VERIFIED, QA execution status is preserved
                if (currentTask.completed() || currentTask.humanVerified()) {
                    targetStatus = currentTask.status();
                }
            }

            String updatedTitle = snapshot.title().isBlank() ? currentTask.title() : snapshot.title();
            String updatedDescription = formatDescription(snapshot);

            TaskBoardTaskUpdateRequest updateRequest = new TaskBoardTaskUpdateRequest(
                    currentTask.taskId(),
                    null,
                    updatedTitle,
                    null,
                    updatedDescription,
                    null,
                    targetStatus,
                    currentTask.priority(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    snapshot.pageUrl(),
                    null
            );

            TaskBoardTask updatedTask = mutationHandler.updateTask(subject, updateRequest);

            NotionTaskSyncState updatedState = existingState.withIngestedRevision(
                    snapshot.lastEditedTime(),
                    false,
                    false,
                    now
            );
            if (conflict) {
                updatedState = updatedState.withConflict(conflictDetails, now);
            }
            syncDataHandler.saveSyncState(updatedState, audit(subject, updatedTask.taskId(), updatedTask.projectKey(),
                    conflict ? "Resolved Notion sync conflict: " + conflictDetails : "Updated task from Notion snapshot", now));

            return new NotionTaskIngestResult(
                    updatedTask.taskId(),
                    snapshot.pageId(),
                    false,
                    true,
                    false,
                    conflict,
                    conflict ? conflictDetails : "Task updated from Notion",
                    updatedTask,
                    updatedState
            );
        }

        // New task ingestion
        TaskBoardTaskStatus initialStatus = mapping.mapInboundStatus(snapshot.status(), snapshot.blocked());
        if (snapshot.archived() || snapshot.deleted()) {
            initialStatus = TaskBoardTaskStatus.CANCELLED;
        }

        String projectKey = mapping.defaultProjectKey();
        if (dataHandler.findProject(projectKey).isEmpty()) {
            // Fallback to project-novus if default doesn't exist
            projectKey = "project-novus";
        }

        String title = snapshot.title().isBlank() ? "Untitled Notion Task" : snapshot.title();
        String description = formatDescription(snapshot);

        TaskBoardTaskCreateRequest createRequest = new TaskBoardTaskCreateRequest(
                projectKey,
                "general",
                title,
                "",
                description,
                "",
                TaskBoardTaskPriority.NORMAL,
                TaskBoardAudience.STAFF,
                1,
                false,
                true,
                "NOTION",
                snapshot.pageUrl(),
                0
        );

        TaskBoardTask createdTask = mutationHandler.createTask(subject, createRequest);
        if (initialStatus != TaskBoardTaskStatus.BACKLOG) {
            createdTask = mutationHandler.updateTask(subject, new TaskBoardTaskUpdateRequest(
                    createdTask.taskId(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    initialStatus,
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

        NotionTaskSyncState newState = new NotionTaskSyncState(
                UUID.randomUUID(),
                createdTask.taskId(),
                snapshot.databaseId(),
                snapshot.pageId(),
                snapshot.pageUrl(),
                snapshot.lastEditedTime(),
                0,
                0,
                false,
                "",
                snapshot.archived(),
                snapshot.deleted(),
                now,
                now
        );

        syncDataHandler.saveSyncState(newState, audit(subject, createdTask.taskId(), projectKey,
                "Created task from Notion page " + snapshot.pageId(), now));

        return new NotionTaskIngestResult(
                createdTask.taskId(),
                snapshot.pageId(),
                true,
                false,
                false,
                false,
                "Task created from Notion snapshot",
                createdTask,
                newState
        );
    }

    @Override
    public List<NotionTaskOutboundChange> findPendingOutboundChanges(QaSubjectContext subject, Optional<String> databaseId) {
        requireAuthorized(subject);
        Instant now = clock.instant();

        if (databaseId != null && databaseId.isPresent() && !databaseId.get().isBlank()) {
            resolveMapping(databaseId.get());
        }

        List<NotionTaskSyncState> pendingStates = syncDataHandler.findPendingOutboundSyncStates(databaseId);
        List<NotionTaskOutboundChange> changes = new ArrayList<>();

        for (NotionTaskSyncState state : pendingStates) {
            Optional<TaskBoardTask> taskOpt = dataHandler.findTask(state.taskId());
            if (taskOpt.isEmpty()) {
                continue;
            }
            TaskBoardTask task = taskOpt.get();
            NotionDatabaseTaskMapping mapping = resolveMapping(state.notionDatabaseId());

            String outboundStatus = mapping.mapOutboundStatus(task.status());
            boolean blocked = task.status() == TaskBoardTaskStatus.BLOCKED;
            boolean evidenceComplete = task.completed() || task.humanVerified();
            String currentStep = task.qaStatus().name();

            changes.add(new NotionTaskOutboundChange(
                    state.syncId(),
                    state.taskId(),
                    state.notionDatabaseId(),
                    state.notionPageId(),
                    state.notionPageUrl(),
                    state.lastOutboundRevision(),
                    outboundStatus,
                    blocked,
                    blocked ? "Task blocked in QA" : "",
                    evidenceComplete,
                    currentStep,
                    now,
                    now
            ));
        }
        return List.copyOf(changes);
    }

    @Override
    public NotionTaskSyncAckResult acknowledgeOutboundSync(QaSubjectContext subject, NotionTaskSyncAckRequest request) {
        Objects.requireNonNull(request, "request");
        requireAuthorized(subject);

        Instant now = clock.instant();
        NotionTaskSyncState state = syncDataHandler.findSyncStateByPageId(request.notionPageId())
                .orElseThrow(() -> new IllegalArgumentException("No sync state found for Notion page: " + request.notionPageId()));

        if (request.syncRevision() <= state.lastAcknowledgedOutboundRevision()) {
            return new NotionTaskSyncAckResult(
                    request.notionPageId(),
                    state.lastAcknowledgedOutboundRevision(),
                    true,
                    "Stale or duplicate acknowledgement ignored"
            );
        }

        NotionTaskSyncState updated = state.withAcknowledgedOutboundRevision(request.syncRevision(), now);
        syncDataHandler.saveSyncState(updated, audit(subject, state.taskId(), "notion-sync",
                "Acknowledged outbound sync revision " + request.syncRevision(), now));

        return new NotionTaskSyncAckResult(
                request.notionPageId(),
                request.syncRevision(),
                false,
                "Outbound sync acknowledged"
        );
    }

    @Override
    public Optional<NotionTaskSyncState> findSyncStateByPageId(QaSubjectContext subject, String notionPageId) {
        Objects.requireNonNull(notionPageId, "notionPageId");
        requireAuthorized(subject);
        return syncDataHandler.findSyncStateByPageId(notionPageId);
    }

    @Override
    public Optional<NotionTaskSyncState> findSyncStateByTaskId(QaSubjectContext subject, UUID taskId) {
        Objects.requireNonNull(taskId, "taskId");
        requireAuthorized(subject);
        return syncDataHandler.findSyncStateByTaskId(taskId);
    }

    @Override
    public void registerDatabaseMapping(QaSubjectContext subject, NotionDatabaseTaskMapping mapping) {
        Objects.requireNonNull(mapping, "mapping");
        requireAuthorized(subject);
        mappingRegistry.registerMapping(mapping);
    }

    @Override
    public Optional<NotionDatabaseTaskMapping> findDatabaseMapping(String databaseId) {
        if (databaseId == null) {
            return Optional.empty();
        }
        return mappingRegistry.findMapping(databaseId);
    }

    public org.tavall.qa.domain.notion.INotionTaskMappingRegistry mappingRegistry() {
        return mappingRegistry;
    }

    public void advanceOutboundRevision(UUID taskId) {
        syncDataHandler.findSyncStateByTaskId(taskId).ifPresent(state -> {
            Instant now = clock.instant();
            NotionTaskSyncState advanced = state.withAdvancedOutboundRevision(state.lastOutboundRevision() + 1, now);
            syncDataHandler.saveSyncState(advanced, null);
        });
    }

    private NotionDatabaseTaskMapping resolveMapping(String databaseId) {
        if (databaseId == null || databaseId.isBlank()) {
            throw new IllegalArgumentException("Notion databaseId must not be null or blank");
        }
        return mappingRegistry.findMapping(databaseId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown or unconfigured Notion database: " + databaseId));
    }

    private void requireAuthorized(QaSubjectContext subject) {
        Objects.requireNonNull(subject, "subject");
        if (!subject.hasQaServiceContext()) {
            throw new SecurityException("Unauthorized: active QA service context required");
        }
        boolean authorized = subject.isGlobalQaAdministrator()
                || subject.canManageTasks()
                || subject.hasCapability(QaCapability.TECHNICAL_DEV_EXECUTION);
        if (!authorized) {
            throw new SecurityException("Unauthorized caller: MANAGE_TASKS or TECHNICAL_DEV_EXECUTION required for Notion sync");
        }
    }

    private static String formatDescription(NotionTaskSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        if (!snapshot.currentStep().isBlank()) {
            sb.append("Current Step: ").append(snapshot.currentStep()).append("\n");
        }
        if (!snapshot.checklistProgress().isBlank()) {
            sb.append("Checklist: ").append(snapshot.checklistProgress()).append("\n");
        }
        if (snapshot.blocked() && !snapshot.blockerSummary().isBlank()) {
            sb.append("Blocker: ").append(snapshot.blockerSummary()).append("\n");
        }
        if (!snapshot.domain().isBlank()) {
            sb.append("Domain: ").append(snapshot.domain()).append("\n");
        }
        if (!snapshot.linearId().isBlank()) {
            sb.append("Linear: ").append(snapshot.linearId()).append(" (").append(snapshot.linearParentUrl()).append(")\n");
        }
        return sb.toString().trim();
    }

    private static TaskBoardAuditEntry audit(
            QaSubjectContext subject,
            UUID taskId,
            String projectKey,
            String summary,
            Instant now
    ) {
        return new TaskBoardAuditEntry(
                UUID.randomUUID(),
                Optional.ofNullable(taskId),
                projectKey,
                subject.accountId(),
                subject.linkedMinecraftUuid().orElse(null),
                TaskBoardAuditEventType.TASK_UPDATED,
                summary,
                now
        );
    }
}
