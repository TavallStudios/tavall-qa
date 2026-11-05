package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpsertRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** External-key upsert acceptance through QA handlers and the isolated native PostgreSQL test fixture. */
@Execution(ExecutionMode.SAME_THREAD)
class TaskBoardExternalKeyUpsertPostgresIntegrationTest {
    @TempDir static Path directory;
    private static TaskBoardPostgresFixture postgres;
    private TaskBoardPostgresComposition composition;

    @BeforeAll
    static void start() throws Exception {
        postgres = new TaskBoardPostgresFixture(directory);
    }

    @AfterAll
    static void stop() throws Exception {
        if (postgres != null) postgres.close();
    }

    @AfterEach
    void closeComposition() {
        if (composition != null) {
            composition.close();
            composition = null;
        }
    }

    @Test
    void concurrentAndRepeatedUpsertsKeepOneRowAndOneAuditPerChange() throws Exception {
        composition = new TaskBoardPostgresComposition(postgres.database(), java.time.Clock.systemUTC());
        ITaskBoardDataHandler data = composition.bean(ITaskBoardDataHandler.class);
        ITaskBoardMutationHandler mutations = composition.bean(ITaskBoardMutationHandler.class);
        var manager = TaskBoardTestSubjects.manager(UUID.randomUUID(), UUID.randomUUID());
        String projectKey = "upsert-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        mutations.createProject(manager, new TaskBoardProjectCreateRequest(projectKey, "External key upsert",
                "Isolated database integration", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.STAFF, 0));
        TaskBoardTaskUpsertRequest request = request(projectKey, "Snapshot one");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = workers.submit(() -> upsert(mutations, manager, request, ready, start));
            var second = workers.submit(() -> upsert(mutations, manager, request, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var one = first.get(15, TimeUnit.SECONDS);
            var two = second.get(15, TimeUnit.SECONDS);
            assertEquals(1, (one.created() ? 1 : 0) + (two.created() ? 1 : 0));
            assertEquals(one.task().taskId(), two.task().taskId());
        }

        var taskId = data.findTasks(projectKey).getFirst().taskId();
        assertEquals(1, data.findTasks(projectKey).size());
        assertEquals(1, data.findAuditEntries(taskId).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.TASK_UPSERTED).count());
        var replay = mutations.upsertTask(manager, request);
        assertFalse(replay.created());
        assertFalse(replay.updated());
        assertEquals(1, data.findTasks(projectKey).size());

        var changed = mutations.upsertTask(manager, request(projectKey, "Snapshot two"));
        assertTrue(changed.updated());
        assertEquals(taskId, changed.task().taskId());
        assertEquals(1, changed.task().qaRevision());
        assertEquals(1, data.findTasks(projectKey).size());
        assertEquals(2, data.findAuditEntries(taskId).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.TASK_UPSERTED).count());
        var changedReplay = mutations.upsertTask(manager, request(projectKey, "Snapshot two"));
        assertFalse(changedReplay.updated());
        assertEquals(2, data.findAuditEntries(taskId).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.TASK_UPSERTED).count());

        mutations.updateTask(manager, new TaskBoardTaskUpdateRequest(taskId, null, null, null, null, null,
                TaskBoardTaskStatus.BLOCKED, null, null, null, null, null, null, null, null, null));
        var restored = mutations.upsertTask(manager, request(projectKey, "Snapshot two"));
        assertTrue(restored.updated());
        assertEquals(taskId, restored.task().taskId());
        assertEquals(1, data.findTasks(projectKey).size(), "Regular Task Board edits must preserve external-key identity");

        var preexisting = mutations.createTask(manager, new TaskBoardTaskCreateRequest(projectKey, "legacy",
                "Previously registered worker", "Previously registered worker", "old scope", "old scope",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.STAFF, 1, false, false, "ledger",
                "source://preexisting", 0));
        var linked = mutations.upsertTask(manager, new TaskBoardTaskUpsertRequest("ledger/preexisting",
                new TaskBoardTaskCreateRequest(projectKey, "legacy", "Previously registered worker",
                        "Previously registered worker", "old scope", "old scope", TaskBoardTaskPriority.NORMAL,
                        TaskBoardAudience.STAFF, 1, false, false, "ledger", "source://preexisting", 0),
                TaskBoardTaskStatus.BACKLOG, preexisting.taskId()));
        assertTrue(linked.updated(), "Linking an existing QA task to its stable key records the identity mapping");
        assertEquals(preexisting.taskId(), linked.task().taskId());
        var linkedReplay = mutations.upsertTask(manager, new TaskBoardTaskUpsertRequest("ledger/preexisting",
                new TaskBoardTaskCreateRequest(projectKey, "legacy", "Previously registered worker",
                        "Previously registered worker", "old scope", "old scope", TaskBoardTaskPriority.NORMAL,
                        TaskBoardAudience.STAFF, 1, false, false, "ledger", "source://preexisting", 0),
                TaskBoardTaskStatus.BACKLOG, preexisting.taskId()));
        assertFalse(linkedReplay.created());
        assertFalse(linkedReplay.updated());
        assertEquals(preexisting.taskId(), linkedReplay.task().taskId());
        assertEquals(2, data.findTasks(projectKey).size(), "Linking a prior QA task must not create a duplicate");

        var linkedReplayWithoutIdHint = mutations.upsertTask(manager, new TaskBoardTaskUpsertRequest("ledger/preexisting",
                new TaskBoardTaskCreateRequest(projectKey, "legacy", "Previously registered worker",
                        "Previously registered worker", "old scope", "old scope", TaskBoardTaskPriority.NORMAL,
                        TaskBoardAudience.STAFF, 1, false, false, "ledger", "source://preexisting", 0),
                TaskBoardTaskStatus.BACKLOG));
        assertFalse(linkedReplayWithoutIdHint.created());
        assertFalse(linkedReplayWithoutIdHint.updated());
        assertEquals(preexisting.taskId(), linkedReplayWithoutIdHint.task().taskId());

        var changedWithoutIdHint = mutations.upsertTask(manager, new TaskBoardTaskUpsertRequest("ledger/preexisting",
                new TaskBoardTaskCreateRequest(projectKey, "legacy", "Previously registered worker",
                        "Previously registered worker", "updated scope", "updated scope", TaskBoardTaskPriority.NORMAL,
                        TaskBoardAudience.STAFF, 1, false, false, "ledger", "source://preexisting", 0),
                TaskBoardTaskStatus.READY));
        assertTrue(changedWithoutIdHint.updated());
        assertEquals(preexisting.taskId(), changedWithoutIdHint.task().taskId());
        assertEquals(2, data.findAuditEntries(preexisting.taskId()).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.TASK_UPSERTED).count());
        assertEquals(2, data.findTasks(projectKey).size(), "Keyed updates must preserve linked identity without an ID hint");
    }

    private static org.tavall.qa.domain.TaskBoardTaskUpsertResult upsert(ITaskBoardMutationHandler mutations,
            org.tavall.qa.domain.auth.QaSubjectContext manager, TaskBoardTaskUpsertRequest request,
            CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent upsert start timed out");
        return mutations.upsertTask(manager, request);
    }

    private static TaskBoardTaskUpsertRequest request(String projectKey, String title) {
        return new TaskBoardTaskUpsertRequest("ledger/test/external-key", new TaskBoardTaskCreateRequest(projectKey,
                "reconciliation", title, title, "checkpoint", "checkpoint", TaskBoardTaskPriority.HIGH,
                TaskBoardAudience.STAFF, 1, false, false, "ledger", "source://ledger/test", 0), TaskBoardTaskStatus.READY);
    }
}
