package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.auth.QaTier;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.TaskBoardTaskUpdateRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL processes, Tavall Database mappings, task handlers and scoped Tavall DI. */
@Execution(ExecutionMode.SAME_THREAD)
class TaskBoardExecutionPostgresIntegrationTest {
    @TempDir static Path directory;
    private static TaskBoardExecutionPostgresFixture postgres;
    private static final Instant NOW = Instant.parse("2026-10-26T06:00:00.123456789Z");

    @BeforeAll
    static void start() throws Exception { postgres = new TaskBoardExecutionPostgresFixture(directory); }

    @AfterAll
    static void stop() throws Exception { if (postgres != null) postgres.close(); }

    @Test
    void checkpointIsIdempotentAppendOnlyAndSurvivesDatabaseProcessRestart() throws Exception {
        Context context = context(null, administrator());
        TaskBoardExecutionLeaseRequest acquire = acquire(context);
        TaskBoardExecutionLeaseData lease = context.handler().acquireLease(context.actor(), acquire);
        assertEquals(lease, context.handler().acquireLease(context.actor(), acquire));
        assertEquals(0, lease.expiresAt().getNano() % 1000, "Returned revisions must use persisted timestamp precision");
        TaskBoardExecutionCheckpointRequest request = checkpoint(lease, UUID.randomUUID(), "b".repeat(64));
        TaskBoardExecutionCheckpointData recorded = context.handler().appendCheckpoint(context.actor(), request);
        assertEquals(recorded, context.handler().appendCheckpoint(context.actor(), request));
        assertEquals(1, context.handler().checkpoints(context.actor(), lease.taskId(), lease.runId(), 10).size());
        assertEquals(1, context.tasks().findAuditEntries(lease.taskId()).stream()
                .filter(event -> event.eventType() == TaskBoardAuditEventType.EXECUTION_CHECKPOINT_RECORDED).count());
        assertThrows(TaskBoardConcurrentModificationException.class, () -> context.handler().appendCheckpoint(context.actor(),
                checkpoint(lease, request.checkpointId(), "c".repeat(64))));
        assertEquals(TaskBoardTaskStatus.BACKLOG, context.tasks().findTask(lease.taskId()).orElseThrow().status(),
                "An execution observation must not accept a QA task");
        try (var connection = DriverManager.getConnection(postgres.url(), "postgres", "");
             var update = connection.prepareStatement("UPDATE task_board_execution_checkpoint SET provider_id = 'other' WHERE checkpoint_id = ?");
             var truncate = connection.createStatement()) {
            update.setString(1, recorded.checkpointId().toString());
            assertThrows(SQLException.class, update::executeUpdate);
            assertThrows(SQLException.class, () -> truncate.execute("TRUNCATE task_board_execution_checkpoint"));
        }
        postgres.restart();
        Context restored = context(context.task(), context.actor());
        assertEquals(recorded, restored.handler().checkpoints(restored.actor(), lease.taskId(), lease.runId(), 10).getFirst());
        assertEquals("tavall-storage://artifacts/sha256/" + "b".repeat(64), recorded.payloadReference().toString());
    }

    @Test
    void concurrentClaimsAndCheckpointWritersCannotSilentlyOverwriteEachOther() throws Exception {
        Context context = context(null, administrator());
        CountDownLatch start = new CountDownLatch(1);
        try (var workers = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = workers.submit(() -> claim(context, acquire(context), start));
            var second = workers.submit(() -> claim(context, acquire(context), start));
            start.countDown();
            assertEquals(1, (first.get(15, TimeUnit.SECONDS) ? 1 : 0) + (second.get(15, TimeUnit.SECONDS) ? 1 : 0));
            TaskBoardExecutionLeaseData lease = context.handler().currentLease(context.actor(), context.task().taskId()).orElseThrow();
            CountDownLatch checkpointStart = new CountDownLatch(1);
            var one = workers.submit(() -> append(context, checkpoint(lease, UUID.randomUUID(), "b".repeat(64)), checkpointStart));
            var two = workers.submit(() -> append(context, checkpoint(lease, UUID.randomUUID(), "c".repeat(64)), checkpointStart));
            checkpointStart.countDown();
            assertEquals(1, (one.get(15, TimeUnit.SECONDS) ? 1 : 0) + (two.get(15, TimeUnit.SECONDS) ? 1 : 0));
            assertEquals(1, context.handler().checkpoints(context.actor(), lease.taskId(), lease.runId(), 100).size());
        }
    }

    @Test
    void expiryTaskChangeAndCurrentAuthorizationFenceResumption() {
        Context context = context(null, administrator());
        TaskBoardExecutionLeaseData lease = context.handler().acquireLease(context.actor(), acquire(context));
        context.handler().appendCheckpoint(context.actor(), checkpoint(lease, UUID.randomUUID(), "b".repeat(64)));
        TaskBoardExecutionLeaseData latest = context.handler().currentLease(context.actor(), lease.taskId()).orElseThrow();
        assertThrows(TaskBoardConcurrentModificationException.class,
                () -> context.handler().renewLease(context.actor(), lease, Duration.ofMinutes(1)));
        context.dependencies().replaceInstance(Clock.class, () -> Clock.fixed(latest.expiresAt().plusSeconds(1), ZoneOffset.UTC));
        assertThrows(TaskBoardConcurrentModificationException.class,
                () -> context.handler().appendCheckpoint(context.actor(), checkpoint(latest, UUID.randomUUID(), "c".repeat(64))));
        TaskBoardExecutionLeaseData resumed = context.handler().acquireLease(context.actor(), new TaskBoardExecutionLeaseRequest(
                lease.taskId(), lease.runId(), UUID.randomUUID(), lease.taskFingerprint(), Duration.ofMinutes(1)));
        assertEquals(1, resumed.checkpointSequence(), "New lease must continue the same run history");
        assertThrows(TaskBoardConcurrentModificationException.class,
                () -> context.handler().appendCheckpoint(context.actor(), checkpoint(latest, UUID.randomUUID(), "c".repeat(64))));
        context.mutations().updateTask(context.actor(), new TaskBoardTaskUpdateRequest(context.task().taskId(),
                null, null, null, "Changed acceptance instructions", null,
                null, null, null, null, null, null, null, null, null, null));
        assertThrows(TaskBoardConcurrentModificationException.class,
                () -> context.handler().appendCheckpoint(context.actor(), checkpoint(resumed, UUID.randomUUID(), "c".repeat(64))));
        assertFalse(context.handler().releaseLease(context.actor(), resumed)
                .activeAt(context.dependencies().getInstance(Clock.class).instant()));
        assertThrows(SecurityException.class,
                () -> context.handler().checkpoints(QaSubjectContext.anonymous(), lease.taskId(), lease.runId(), 10));
    }

    @Test
    void technicalAuthorityIsRecheckedAndGenerationRetirementFailsClosed() {
        Context manager = context(null, administrator());
        UUID account = UUID.randomUUID();
        QaSubjectContext assigned = TaskBoardTestSubjects.active(account, UUID.randomUUID(),
                Set.of(QaClass.REGULAR, QaClass.TECHNICAL), Optional.of(QaTier.TIER_III), Set.of(QaEnvironmentGrant.DEV),
                Set.of(QaCapability.TECHNICAL_DEV_EXECUTION),
                List.of(TaskBoardTestSubjects.assignment(manager.task().taskId(), QaCapability.TECHNICAL_DEV_EXECUTION, QaEnvironmentGrant.DEV)),
                true, false);
        Context context = context(manager.task(), assigned);
        TaskBoardExecutionLeaseData lease = context.handler().acquireLease(assigned, acquire(context));
        QaSubjectContext revoked = TaskBoardTestSubjects.active(account, assigned.linkedMinecraftUuid().orElseThrow(),
                Set.of(QaClass.REGULAR, QaClass.TECHNICAL), Optional.of(QaTier.TIER_III), Set.of(QaEnvironmentGrant.DEV),
                Set.of(), List.of(), true, false);
        assertThrows(SecurityException.class,
                () -> context.handler().appendCheckpoint(revoked, checkpoint(lease, UUID.randomUUID(), "b".repeat(64))));
        assertThrows(SecurityException.class, () -> context.handler().currentLease(assigned, UUID.randomUUID()));
        context.dependencies().removeDependency(ITaskBoardExecutionCheckpointHandler.class);
        assertThrows(IllegalStateException.class, () -> context.handler().currentLease(assigned, lease.taskId()));
    }

    private static boolean claim(Context context, TaskBoardExecutionLeaseRequest request, CountDownLatch start) throws Exception {
        start.await();
        try { context.handler().acquireLease(context.actor(), request); return true; }
        catch (TaskBoardConcurrentModificationException expected) { return false; }
    }

    private static boolean append(Context context, TaskBoardExecutionCheckpointRequest request, CountDownLatch start) throws Exception {
        start.await();
        try { context.handler().appendCheckpoint(context.actor(), request); return true; }
        catch (TaskBoardConcurrentModificationException expected) { return false; }
    }

    private static TaskBoardExecutionLeaseRequest acquire(Context context) {
        return new TaskBoardExecutionLeaseRequest(context.task().taskId(), UUID.randomUUID(), UUID.randomUUID(),
                TaskBoardTaskFingerprint.compute(context.task()), Duration.ofMinutes(1));
    }

    private static TaskBoardExecutionCheckpointRequest checkpoint(TaskBoardExecutionLeaseData lease, UUID id, String payload) {
        return new TaskBoardExecutionCheckpointRequest(id, lease,
                new TaskBoardExecutionSourceData("TavallStudios/tavall-ai", "working/fixture", "a".repeat(40), "d".repeat(64)),
                payload, "codex", TaskBoardExecutionStatus.COMPLETED);
    }

    private static QaSubjectContext administrator() {
        return TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(), Set.of(), Optional.empty(),
                Set.of(QaEnvironmentGrant.DEV), Set.of(), List.of(), false, true);
    }

    private static Context context(TaskBoardTask existing, QaSubjectContext actor) {
        TaskBoardDataHandler tasks = new TaskBoardDataHandler(postgres.database());
        TaskBoardMutationHandler mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());
        TaskBoardTask task = existing;
        if (task == null) {
            String project = "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            mutations.createProject(actor, new TaskBoardProjectCreateRequest(project, "Execution fixture", "Private local database",
                    TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));
            task = mutations.createTask(actor, new TaskBoardTaskCreateRequest(project, "execution", "Checkpoint task", "Checkpoint task",
                    "Acceptance instructions", "Public summary", TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC,
                    1, false, false, "test", "https://example.invalid/source", 0));
        }
        DependencyMap dependencies = new DependencyMap();
        dependencies.registerInstance(IPostgresDatabase.class, postgres.database());
        dependencies.registerInstance(ITaskBoardDataHandler.class, tasks);
        dependencies.registerInstance(Clock.class, Clock.fixed(NOW, ZoneOffset.UTC));
        dependencies.registerInstance(ITaskBoardExecutionCheckpointDataHandler.class, new TaskBoardExecutionCheckpointDataHandler());
        ITaskBoardExecutionCheckpointHandler handler = dependencies.registerInstance(ITaskBoardExecutionCheckpointHandler.class,
                new TaskBoardExecutionCheckpointHandler());
        return new Context(dependencies, handler, tasks, mutations, task, actor);
    }

    private record Context(DependencyMap dependencies, ITaskBoardExecutionCheckpointHandler handler,
            TaskBoardDataHandler tasks, TaskBoardMutationHandler mutations, TaskBoardTask task, QaSubjectContext actor) { }
}
