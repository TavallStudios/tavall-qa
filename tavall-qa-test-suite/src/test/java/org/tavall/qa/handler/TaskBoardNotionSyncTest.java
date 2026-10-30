package org.tavall.qa.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.filesystem.api.FilesystemAtomicOperations;
import org.tavall.filesystem.system.atomic.AtomicFilesystemOperations;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardComment;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.notion.NotionDatabaseTaskMapping;
import org.tavall.qa.domain.notion.NotionTaskIngestResult;
import org.tavall.qa.domain.notion.NotionTaskOutboundChange;
import org.tavall.qa.domain.notion.NotionTaskSnapshot;
import org.tavall.qa.domain.notion.NotionTaskSyncAckRequest;
import org.tavall.qa.domain.notion.NotionTaskSyncAckResult;
import org.tavall.qa.domain.notion.NotionTaskSyncState;
import org.tavall.qa.persistence.TaskBoardNotionSyncDataHandler;
import org.tavall.qa.request.TaskBoardAgentExecutionUpdateRequest;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.view.TaskBoardAgentTaskContextView;
import org.tavall.qa.view.TaskBoardAgentTaskView;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardNotionSyncTest {
    private final UUID managerAccountId = UUID.randomUUID();
    private final UUID managerMinecraftUuid = UUID.randomUUID();
    private final UUID devAccountId = UUID.randomUUID();

    @TempDir
    Path tempDir;

    private TaskBoardTestDataHandler dataHandler;
    private TaskBoardNotionSyncDataHandler syncDataHandler;
    private TaskBoardMutationHandler mutationHandler;
    private TaskBoardQueryHandler queryHandler;
    private TaskBoardNotionSyncHandler syncHandler;
    private TaskBoardAgentViewHandler agentViewHandler;

    private QaSubjectContext manager;
    private QaSubjectContext devAgent;
    private QaSubjectContext unauthorizedSubject;

    private final Instant baseTime = Instant.parse("2026-10-30T10:00:00Z");

    @BeforeEach
    void setUp() {
        dataHandler = new TaskBoardTestDataHandler();
        FilesystemAtomicOperations atomicOps = new AtomicFilesystemOperations();
        syncDataHandler = new TaskBoardNotionSyncDataHandler(null, atomicOps, new ObjectMapper(), tempDir);

        mutationHandler = new TaskBoardMutationHandler(dataHandler, new TaskBoardActionTargetResolver());
        queryHandler = new TaskBoardQueryHandler(dataHandler, new TaskBoardActionTargetResolver());

        Clock clock = Clock.fixed(baseTime, ZoneOffset.UTC);
        syncHandler = new TaskBoardNotionSyncHandler(syncDataHandler, dataHandler, mutationHandler, clock);
        agentViewHandler = new TaskBoardAgentViewHandler(queryHandler, mutationHandler, dataHandler, syncHandler);

        manager = TaskBoardTestSubjects.manager(managerAccountId, managerMinecraftUuid);

        devAgent = TaskBoardTestSubjects.active(
                devAccountId,
                null,
                java.util.Set.of(org.tavall.qa.domain.auth.QaClass.REGULAR, org.tavall.qa.domain.auth.QaClass.TECHNICAL),
                Optional.of(org.tavall.qa.domain.auth.QaTier.TIER_III),
                java.util.Set.of(QaEnvironmentGrant.DEV),
                java.util.Set.of(QaCapability.VIEW_QA_TASKS, QaCapability.VIEW_STAFF_TASKS, QaCapability.TECHNICAL_DEV_EXECUTION, QaCapability.SUBMIT_EVIDENCE),
                List.of(),
                true,
                false
        );

        unauthorizedSubject = QaSubjectContext.anonymous();

        mutationHandler.createProject(manager, new TaskBoardProjectCreateRequest(
                "tavall-pvp",
                "Tavall PvP",
                "PvP gameplay QA",
                TaskBoardProjectStatus.ACTIVE,
                TaskBoardAudience.PUBLIC,
                10
        ));
    }

    @Test
    void testCreateTaskFromNotionSnapshot() {
        NotionTaskSnapshot snapshot = new NotionTaskSnapshot(
                "ws-pvp-01",
                "3a338458ddfd80eb9bbfcb8bc741e610",
                "page-pvp-task-101",
                "https://app.notion.com/p/3a338458ddfd80eb9bbfcb8bc741e610-page-101",
                Instant.parse("2026-10-30T09:00:00Z"),
                "Refactor Combat Hitbox Resolution",
                "In progress",
                "DevAgent42",
                Instant.parse("2026-11-05T00:00:00Z"),
                "Step 2: Box intersection bounds",
                "2/5 tests passing",
                "tavall-pvp",
                "Gameplay",
                List.of("Product", "Infrastructure"),
                false,
                "",
                false,
                "LIN-451",
                "https://linear.app/tavall/issue/LIN-451",
                false,
                false
        );

        NotionTaskIngestResult result = syncHandler.ingestSnapshot(manager, snapshot);

        assertTrue(result.created());
        assertFalse(result.updated());
        assertFalse(result.duplicateIngest());
        assertFalse(result.conflict());
        assertNotNull(result.taskId());
        assertEquals("page-pvp-task-101", result.notionPageId());

        TaskBoardTask createdTask = result.task();
        assertEquals("Refactor Combat Hitbox Resolution", createdTask.title());
        assertEquals(TaskBoardTaskStatus.ACTIVE, createdTask.status());
        assertEquals("NOTION", createdTask.sourceType());
        assertEquals("https://app.notion.com/p/3a338458ddfd80eb9bbfcb8bc741e610-page-101", createdTask.sourceReference());

        // Verify sync state
        NotionTaskSyncState state = result.syncState();
        assertEquals("page-pvp-task-101", state.notionPageId());
        assertEquals(Instant.parse("2026-10-30T09:00:00Z"), state.lastIngestedRevision());
        assertEquals(0, state.lastOutboundRevision());
        assertFalse(state.hasConflict());
        assertFalse(state.archived());
        assertFalse(state.deleted());
    }

    @Test
    void testUpdateTaskFromNotionSnapshot() {
        NotionTaskSnapshot initial = sampleSnapshot("page-pvp-task-102", "Initial Title", "Not started",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        NotionTaskIngestResult created = syncHandler.ingestSnapshot(manager, initial);

        NotionTaskSnapshot updatedSnapshot = sampleSnapshot("page-pvp-task-102", "Updated Title", "Done",
                Instant.parse("2026-10-30T09:30:00Z"), false, false);
        NotionTaskIngestResult updated = syncHandler.ingestSnapshot(manager, updatedSnapshot);

        assertFalse(updated.created());
        assertTrue(updated.updated());
        assertFalse(updated.duplicateIngest());
        assertFalse(updated.conflict());
        assertEquals("Updated Title", updated.task().title());
        assertEquals(TaskBoardTaskStatus.DONE, updated.task().status());
        assertEquals(Instant.parse("2026-10-30T09:30:00Z"), updated.syncState().lastIngestedRevision());
    }

    @Test
    void testDuplicateIngestIsIdempotent() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-103", "Stable Title", "Up next",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        NotionTaskIngestResult first = syncHandler.ingestSnapshot(manager, snapshot);
        assertTrue(first.created());

        // Re-ingest exact same snapshot revision
        NotionTaskIngestResult second = syncHandler.ingestSnapshot(manager, snapshot);
        assertFalse(second.created());
        assertFalse(second.updated());
        assertTrue(second.duplicateIngest());
        assertEquals(first.taskId(), second.taskId());

        // Re-ingest with older timestamp
        NotionTaskSnapshot older = sampleSnapshot("page-pvp-task-103", "Old Title", "Not started",
                Instant.parse("2026-10-30T07:00:00Z"), false, false);
        NotionTaskIngestResult third = syncHandler.ingestSnapshot(manager, older);
        assertTrue(third.duplicateIngest());
        assertEquals("Stable Title", third.task().title());
    }

    @Test
    void testConflictResolutionWhenBothNotionAndQaModifyTask() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-104", "Conflict Target", "In progress",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        NotionTaskIngestResult created = syncHandler.ingestSnapshot(manager, snapshot);
        UUID taskId = created.taskId();

        // QA modifies task locally and advances outbound revision
        agentViewHandler.updateAgentExecution(manager, new TaskBoardAgentExecutionUpdateRequest(
                taskId,
                TaskBoardTaskStatus.DONE,
                false,
                "",
                "Completed hitbox tests in QA"
        ));

        // Notion also concurrently submits changes with a newer revision
        NotionTaskSnapshot concurrentEdit = sampleSnapshot("page-pvp-task-104", "Conflict Target (Renamed in Notion)", "Not started",
                Instant.parse("2026-10-30T09:45:00Z"), false, false);

        NotionTaskIngestResult conflictResult = syncHandler.ingestSnapshot(manager, concurrentEdit);

        assertTrue(conflictResult.conflict());
        assertTrue(conflictResult.updated());
        assertTrue(conflictResult.syncState().hasConflict());
        assertTrue(conflictResult.syncState().conflictDetails().contains("Concurrent modification"));

        // Conflict resolution: QA's DONE status was preserved against Notion's "Not started"
        assertEquals(TaskBoardTaskStatus.DONE, conflictResult.task().status());
        assertEquals("Conflict Target (Renamed in Notion)", conflictResult.task().title());
    }

    @Test
    void testArchivedAndDeletedHandling() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-105", "Task To Archive", "In progress",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        syncHandler.ingestSnapshot(manager, snapshot);

        // Ingest archived snapshot
        NotionTaskSnapshot archivedSnapshot = sampleSnapshot("page-pvp-task-105", "Task To Archive", "In progress",
                Instant.parse("2026-10-30T09:00:00Z"), true, false);
        NotionTaskIngestResult archiveResult = syncHandler.ingestSnapshot(manager, archivedSnapshot);

        assertTrue(archiveResult.updated());
        assertEquals(TaskBoardTaskStatus.CANCELLED, archiveResult.task().status());
        assertTrue(archiveResult.syncState().archived());
        assertFalse(archiveResult.syncState().deleted());

        // Ingest deleted snapshot
        NotionTaskSnapshot deletedSnapshot = sampleSnapshot("page-pvp-task-105", "Task To Archive", "In progress",
                Instant.parse("2026-10-30T09:30:00Z"), false, true);
        NotionTaskIngestResult deleteResult = syncHandler.ingestSnapshot(manager, deletedSnapshot);

        assertTrue(deleteResult.updated());
        assertEquals(TaskBoardTaskStatus.CANCELLED, deleteResult.task().status());
        assertTrue(deleteResult.syncState().deleted());
    }

    @Test
    void testOutboundPendingChangesAndAckReplay() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-106", "Outbound Test Task", "Not started",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        NotionTaskIngestResult created = syncHandler.ingestSnapshot(manager, snapshot);

        // Initially no pending outbound changes
        List<NotionTaskOutboundChange> initialPending = syncHandler.findPendingOutboundChanges(manager, Optional.empty());
        assertTrue(initialPending.isEmpty());

        // QA modifies the task and advances outbound revision
        agentViewHandler.updateAgentExecution(manager, new TaskBoardAgentExecutionUpdateRequest(
                created.taskId(),
                TaskBoardTaskStatus.ACTIVE,
                false,
                "",
                "Work started"
        ));

        // Outbound pending changes now contains the change with syncRevision = 1
        List<NotionTaskOutboundChange> pending = syncHandler.findPendingOutboundChanges(manager, Optional.empty());
        assertEquals(1, pending.size());
        NotionTaskOutboundChange change = pending.get(0);
        assertEquals("page-pvp-task-106", change.notionPageId());
        assertEquals("In progress", change.status());
        assertEquals(1, change.syncRevision());

        // Acknowledge sync revision 1
        NotionTaskSyncAckResult ackResult = syncHandler.acknowledgeOutboundSync(manager,
                new NotionTaskSyncAckRequest("page-pvp-task-106", 1, baseTime));
        assertFalse(ackResult.staleOrDuplicate());
        assertEquals(1, ackResult.acknowledgedRevision());

        // Pending changes now empty
        assertTrue(syncHandler.findPendingOutboundChanges(manager, Optional.empty()).isEmpty());

        // Ack replay is an idempotent no-op
        NotionTaskSyncAckResult replayResult = syncHandler.acknowledgeOutboundSync(manager,
                new NotionTaskSyncAckRequest("page-pvp-task-106", 1, baseTime));
        assertTrue(replayResult.staleOrDuplicate());
    }

    @Test
    void testUnauthorizedCallerFailsClosed() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-107", "Sec Test", "Not started",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);

        // Anonymous caller fails closed
        assertThrows(SecurityException.class, () -> syncHandler.ingestSnapshot(unauthorizedSubject, snapshot));
        assertThrows(SecurityException.class, () -> syncHandler.findPendingOutboundChanges(unauthorizedSubject, Optional.empty()));
        assertThrows(SecurityException.class, () -> syncHandler.acknowledgeOutboundSync(unauthorizedSubject,
                new NotionTaskSyncAckRequest("page-pvp-task-107", 1, baseTime)));

        // Unprivileged active subject without MANAGE_TASKS or TECHNICAL_DEV_EXECUTION
        QaSubjectContext regularUser = TaskBoardTestSubjects.active(
                UUID.randomUUID(),
                null,
                java.util.Set.of(org.tavall.qa.domain.auth.QaClass.REGULAR),
                Optional.of(org.tavall.qa.domain.auth.QaTier.TIER_I),
                java.util.Set.of(),
                java.util.Set.of(QaCapability.VIEW_QA_TASKS),
                List.of(),
                false,
                false
        );
        assertThrows(SecurityException.class, () -> syncHandler.ingestSnapshot(regularUser, snapshot));
    }

    @Test
    void testAgentRoleAwareTaskViewsAndContextWithoutWorkspaceDump() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-108", "Bounded Context Task", "In progress",
                Instant.parse("2026-10-30T08:00:00Z"), false, false);
        NotionTaskIngestResult created = syncHandler.ingestSnapshot(manager, snapshot);
        UUID taskId = created.taskId();

        // Dev agent can discover authorized tasks
        List<TaskBoardAgentTaskView> agentTasks = agentViewHandler.findAuthorizedAgentTasks(devAgent, Optional.empty());
        assertFalse(agentTasks.isEmpty());
        assertTrue(agentTasks.stream().anyMatch(t -> t.taskId().equals(taskId)));

        // Dev agent can retrieve exact task-specific context without a workspace dump
        Optional<TaskBoardAgentTaskContextView> contextOpt = agentViewHandler.findAgentTaskContext(devAgent, taskId);
        assertTrue(contextOpt.isPresent());
        TaskBoardAgentTaskContextView context = contextOpt.get();
        assertEquals(taskId, context.task().taskId());
        assertTrue(context.syncState().isPresent());
        assertEquals("page-pvp-task-108", context.syncState().get().notionPageId());
        assertEquals("tavall-pvp", context.planningContext().get("projectKey"));

        // Dev agent can update execution and add notes
        TaskBoardTask updated = agentViewHandler.updateAgentExecution(devAgent, new TaskBoardAgentExecutionUpdateRequest(
                taskId,
                TaskBoardTaskStatus.ACTIVE,
                true,
                "Waiting on upstream dependency",
                "Executed preliminary tests, blocker identified"
        ));
        assertEquals(TaskBoardTaskStatus.BLOCKED, updated.status());

        // Verify QA note comment was added to the task
        List<TaskBoardComment> comments = dataHandler.findComments(taskId);
        assertEquals(1, comments.size());
        assertEquals("Executed preliminary tests, blocker identified", comments.get(0).body());

        // Unauthorized caller cannot access task context
        assertTrue(agentViewHandler.findAgentTaskContext(unauthorizedSubject, taskId).isEmpty());
    }

    @Test
    void testCustomPerDatabaseMappingConfiguration() {
        // Configure a custom database mapping for Kingdoms
        Map<String, TaskBoardTaskStatus> customStatus = Map.of(
                "Planning", TaskBoardTaskStatus.BACKLOG,
                "Developing", TaskBoardTaskStatus.ACTIVE,
                "Shipped", TaskBoardTaskStatus.DONE
        );
        Map<TaskBoardTaskStatus, String> customOutbound = Map.of(
                TaskBoardTaskStatus.BACKLOG, "Planning",
                TaskBoardTaskStatus.ACTIVE, "Developing",
                TaskBoardTaskStatus.DONE, "Shipped"
        );
        NotionDatabaseTaskMapping customMapping = new NotionDatabaseTaskMapping(
                "kingdoms-db-999",
                "Custom Kingdoms",
                "tavall-pvp",
                "Task Title",
                "State",
                customStatus,
                customOutbound,
                "Assignee",
                "Target Date",
                "Step",
                "Progress",
                "Is Blocked",
                "Reason",
                "Finished",
                "Feature",
                "Area",
                "Tags",
                "Ticket",
                "URL",
                "Reconciled"
        );

        syncHandler.registerDatabaseMapping(manager, customMapping);

        // Ingest snapshot using custom mapping
        NotionTaskSnapshot customSnapshot = new NotionTaskSnapshot(
                "ws-kingdoms",
                "kingdoms-db-999",
                "page-k-01",
                "https://notion.so/page-k-01",
                Instant.parse("2026-10-30T09:00:00Z"),
                "Build Fortress Walls",
                "Developing",
                "ArchitectBob",
                null,
                "",
                "",
                "Kingdoms Defense",
                "Building",
                List.of(),
                false,
                "",
                false,
                "",
                "",
                false,
                false
        );

        NotionTaskIngestResult result = syncHandler.ingestSnapshot(manager, customSnapshot);
        assertTrue(result.created());
        assertEquals(TaskBoardTaskStatus.ACTIVE, result.task().status());
        assertEquals("Build Fortress Walls", result.task().title());
    }

    @Test
    void testRestartSurvivingFileState() {
        NotionTaskSnapshot snapshot = sampleSnapshot("page-pvp-task-restart", "Restart Persistence", "In progress",
                Instant.parse("2026-10-30T09:00:00Z"), false, false);
        NotionTaskIngestResult created = syncHandler.ingestSnapshot(manager, snapshot);
        UUID taskId = created.taskId();

        // Simulate application restart with a fresh TaskBoardNotionSyncDataHandler pointing to the same directory
        FilesystemAtomicOperations atomicOps = new AtomicFilesystemOperations();
        TaskBoardNotionSyncDataHandler recoveredDataHandler = new TaskBoardNotionSyncDataHandler(
                null, atomicOps, new ObjectMapper(), tempDir
        );

        Optional<NotionTaskSyncState> recoveredState = recoveredDataHandler.findSyncStateByPageId("page-pvp-task-restart");
        assertTrue(recoveredState.isPresent(), "Sync state must survive restart via file-backed mirror");
        assertEquals(taskId, recoveredState.get().taskId());
        assertEquals("page-pvp-task-restart", recoveredState.get().notionPageId());
        assertEquals(Instant.parse("2026-10-30T09:00:00Z"), recoveredState.get().lastIngestedRevision());
    }

    private NotionTaskSnapshot sampleSnapshot(
            String pageId,
            String title,
            String status,
            Instant lastEdited,
            boolean archived,
            boolean deleted
    ) {
        return new NotionTaskSnapshot(
                "ws-pvp-01",
                "3a338458ddfd80eb9bbfcb8bc741e610",
                pageId,
                "https://app.notion.com/p/" + pageId,
                lastEdited,
                title,
                status,
                "DevAgent",
                null,
                "Step 1",
                "1/1",
                "tavall-pvp",
                "Gameplay",
                List.of(),
                false,
                "",
                false,
                "",
                "",
                archived,
                deleted
        );
    }
}
