package org.tavall.qa.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.maps.DependencyMap;
import org.tavall.filesystem.api.FilesystemAtomicOperations;
import org.tavall.filesystem.system.atomic.AtomicFilesystemOperations;
import org.tavall.qa.auth.TaskBoardTestSubjects;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardProjectStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.domain.execution.TaskBoardExecutionDiffData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDrift;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDriftType;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorReconcileResult;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorRecordType;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorSettings;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorVerificationReport;
import org.tavall.qa.persistence.ITaskBoardDataHandler;
import org.tavall.qa.persistence.ITaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.persistence.TaskBoardDataHandler;
import org.tavall.qa.persistence.TaskBoardExecutionCheckpointDataHandler;
import org.tavall.qa.request.TaskBoardProjectCreateRequest;
import org.tavall.qa.request.TaskBoardTaskCreateRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Behaviour tests for file redundancy mirror on PostgreSQL-authoritative Task Board state:
 * - Mirror written after commit
 * - Not on rollback
 * - Mirror write failure does not roll back PostgreSQL transaction (recorded as drift)
 * - Drift detected and reconciled (PostgreSQL authoritative)
 * - Torn / corrupt file quarantined
 * - Never read mirror in normal request paths
 */
@Execution(ExecutionMode.SAME_THREAD)
class TaskBoardFileMirrorIntegrationTest {
    @TempDir static Path postgresDir;
    @TempDir Path mirrorRoot;

    private static TaskBoardExecutionPostgresFixture postgres;
    private static final Instant NOW = Instant.parse("2026-10-26T08:00:00.123456789Z");

    private DependencyMap dependencies;
    private TaskBoardDataHandler tasks;
    private TaskBoardMutationHandler mutations;
    private ITaskBoardExecutionCheckpointDataHandler checkpointData;
    private ITaskBoardExecutionCheckpointHandler checkpointHandler;
    private TaskBoardFileMirrorHandler mirrorHandler;
    private QaSubjectContext administrator;
    private FilesystemAtomicOperations atomicOps;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startPostgres() throws Exception {
        postgres = new TaskBoardExecutionPostgresFixture(postgresDir);
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @BeforeEach
    void setUp() {
        administrator = TaskBoardTestSubjects.active(UUID.randomUUID(), UUID.randomUUID(), Set.of(), Optional.empty(),
                Set.of(QaEnvironmentGrant.DEV), Set.of(), List.of(), false, true);

        atomicOps = new AtomicFilesystemOperations();
        objectMapper = new ObjectMapper();

        dependencies = new DependencyMap();
        dependencies.registerInstance(IPostgresDatabase.class, postgres.database());
        dependencies.registerInstance(FilesystemAtomicOperations.class, atomicOps);
        dependencies.registerInstance(ObjectMapper.class, objectMapper);
        dependencies.registerInstance(Clock.class, Clock.fixed(NOW, ZoneOffset.UTC));
        dependencies.registerInstance(TaskBoardFileMirrorSettings.class, new TaskBoardFileMirrorSettings(mirrorRoot));

        mirrorHandler = new TaskBoardFileMirrorHandler();
        dependencies.registerInstance(ITaskBoardFileMirrorHandler.class, mirrorHandler);
        dependencies.registerInstance(TaskBoardFileMirrorHandler.class, mirrorHandler);

        tasks = new TaskBoardDataHandler(postgres.database());
        dependencies.registerInstance(ITaskBoardDataHandler.class, tasks);
        dependencies.registerInstance(TaskBoardDataHandler.class, tasks);

        mutations = new TaskBoardMutationHandler(tasks, new TaskBoardActionTargetResolver());

        checkpointData = new TaskBoardExecutionCheckpointDataHandler();
        dependencies.registerInstance(ITaskBoardExecutionCheckpointDataHandler.class, checkpointData);
        dependencies.registerInstance(TaskBoardExecutionCheckpointDataHandler.class, (TaskBoardExecutionCheckpointDataHandler) checkpointData);

        checkpointHandler = new TaskBoardExecutionCheckpointHandler();
        dependencies.registerInstance(ITaskBoardExecutionCheckpointHandler.class, checkpointHandler);
        dependencies.registerInstance(TaskBoardExecutionCheckpointHandler.class, (TaskBoardExecutionCheckpointHandler) checkpointHandler);
    }

    @Test
    void mirrorWrittenAfterCommit() throws Exception {
        String projectKey = "proj-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        mutations.createProject(administrator, new TaskBoardProjectCreateRequest(projectKey, "Test Project",
                "Mirror test project", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));

        TaskBoardTask task = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "mirror-task",
                "Task to be mirrored", "Task to be mirrored", "Mirror instructions", "Mirror summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 1, false, false, "test",
                "https://example.invalid/source", 0));

        Path taskFile = mirrorRoot.resolve("qa/task/" + task.taskId() + ".json");
        assertTrue(Files.exists(taskFile), "Mirror task file must exist after commit");

        JsonNode taskJson = objectMapper.readTree(Files.readString(taskFile, StandardCharsets.UTF_8));
        assertEquals(1, taskJson.get("schemaVersion").asInt());
        assertEquals("qa", taskJson.get("ownerIdentity").asText());
        assertEquals("task", taskJson.get("recordType").asText());
        assertEquals(task.taskId().toString(), taskJson.get("recordId").asText());
        assertNotNull(taskJson.get("payloadSha256").asText());
        assertEquals("Task to be mirrored", taskJson.get("payload").get("title").asText());

        TaskBoardExecutionLeaseRequest leaseRequest = new TaskBoardExecutionLeaseRequest(task.taskId(),
                UUID.randomUUID(), UUID.randomUUID(), TaskBoardTaskFingerprint.compute(task), Duration.ofMinutes(5));
        TaskBoardExecutionLeaseData lease = checkpointHandler.acquireLease(administrator, leaseRequest);

        Path leaseFile = mirrorRoot.resolve("qa/execution-lease/" + task.taskId() + ".json");
        assertTrue(Files.exists(leaseFile), "Mirror lease file must exist after commit");

        JsonNode leaseJson = objectMapper.readTree(Files.readString(leaseFile, StandardCharsets.UTF_8));
        assertEquals("execution-lease", leaseJson.get("recordType").asText());
        assertEquals(task.taskId().toString(), leaseJson.get("recordId").asText());
        assertEquals(lease.runId().toString(), leaseJson.get("payload").get("runId").asText());
        assertEquals(lease.revision(), leaseJson.get("payload").get("revision").asLong());

        UUID checkpointId = UUID.randomUUID();
        TaskBoardExecutionCheckpointPayloadData payload = samplePayload("Mirror instructions", "next-step");
        TaskBoardExecutionCheckpointRequest checkpointRequest = new TaskBoardExecutionCheckpointRequest(
                checkpointId, lease,
                new TaskBoardExecutionSourceData("TavallStudios/tavall-qa", "working/qa-mirror", "1".repeat(40),
                        payload.diff().sha256()),
                payload, "codex", TaskBoardExecutionStatus.COMPLETED);
        TaskBoardExecutionCheckpointData checkpoint = checkpointHandler.appendCheckpoint(administrator, checkpointRequest);

        Path checkpointFile = mirrorRoot.resolve("qa/checkpoint/" + checkpointId + ".json");
        assertTrue(Files.exists(checkpointFile), "Mirror checkpoint file must exist after commit");

        JsonNode checkpointJson = objectMapper.readTree(Files.readString(checkpointFile, StandardCharsets.UTF_8));
        assertEquals("checkpoint", checkpointJson.get("recordType").asText());
        assertEquals(checkpointId.toString(), checkpointJson.get("recordId").asText());
        assertEquals(checkpoint.sequence(), checkpointJson.get("payload").get("sequence").asLong());

        JsonNode updatedLeaseJson = objectMapper.readTree(Files.readString(leaseFile, StandardCharsets.UTF_8));
        assertEquals(lease.revision() + 1, updatedLeaseJson.get("payload").get("revision").asLong());
        assertEquals(checkpoint.sequence(), updatedLeaseJson.get("payload").get("checkpointSequence").asLong());
    }

    @Test
    void notOnRollback() {
        String projectKey = "proj-rb-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        mutations.createProject(administrator, new TaskBoardProjectCreateRequest(projectKey, "Rollback Project",
                "Rollback test project", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));

        TaskBoardTask task = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "task-rb",
                "Rollback task", "Rollback task", "Instructions", "Summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 1, false, false, "test",
                "https://example.invalid/source", 0));

        TaskBoardExecutionLeaseRequest invalidAcquire = new TaskBoardExecutionLeaseRequest(task.taskId(),
                UUID.randomUUID(), UUID.randomUUID(), "0".repeat(64), Duration.ofMinutes(5));

        assertThrows(TaskBoardConcurrentModificationException.class, () ->
                checkpointHandler.acquireLease(administrator, invalidAcquire));

        Path leaseFile = mirrorRoot.resolve("qa/execution-lease/" + task.taskId() + ".json");
        assertFalse(Files.exists(leaseFile), "Mirror lease file must not exist when transaction rolls back");
    }

    @Test
    void mirrorWriteFailureDoesNotRollbackCommittedPostgresTransaction() {
        String projectKey = "proj-fail-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mutations.createProject(administrator, new TaskBoardProjectCreateRequest(projectKey, "Fail Project",
                "Fail project", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));

        mirrorHandler.setSimulateWriteFailure(true);

        TaskBoardTask task = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "fail-task",
                "Task with simulated mirror failure", "Task description", "Instructions", "Summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 1, false, false, "test",
                "https://example.invalid/source", 0));

        assertNotNull(task, "PostgreSQL transaction committed successfully despite mirror failure");
        assertTrue(tasks.findTask(task.taskId()).isPresent(), "Task must exist in PostgreSQL");

        Path taskFile = mirrorRoot.resolve("qa/task/" + task.taskId() + ".json");
        assertFalse(Files.exists(taskFile), "Mirror file was not written due to failure");

        List<TaskBoardFileMirrorDrift> recorded = mirrorHandler.recordedDrifts();
        assertFalse(recorded.isEmpty(), "Drift must be recorded when mirror write fails");
        assertTrue(recorded.stream().anyMatch(d -> d.recordId().equals(task.taskId().toString())
                && d.driftType() == TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR));

        mirrorHandler.setSimulateWriteFailure(false);
        TaskBoardFileMirrorReconcileResult reconcileResult = mirrorHandler.reconcile();
        assertTrue(reconcileResult.driftsResolved() >= 1, "Reconcile must recover mirror file from PostgreSQL");
        assertTrue(Files.exists(taskFile), "Mirror file must now exist after reconcile");
    }

    @Test
    void driftDetectedAndReconciled() throws IOException {
        String projectKey = "proj-drift-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mutations.createProject(administrator, new TaskBoardProjectCreateRequest(projectKey, "Drift Project",
                "Drift project", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));

        TaskBoardTask task1 = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "drift-1",
                "Task 1", "Task 1", "Instructions", "Summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 1, false, false, "test",
                "https://example.invalid/source", 0));

        TaskBoardTask task2 = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "drift-2",
                "Task 2", "Task 2", "Instructions", "Summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 2, false, false, "test",
                "https://example.invalid/source", 0));

        Path task1File = mirrorRoot.resolve("qa/task/" + task1.taskId() + ".json");
        assertTrue(Files.deleteIfExists(task1File), "Delete mirror file to create MISSING_IN_MIRROR drift");

        Path task2File = mirrorRoot.resolve("qa/task/" + task2.taskId() + ".json");
        String content = Files.readString(task2File, StandardCharsets.UTF_8);
        String tamperedContent = content.replace("\"Task 2\"", "\"Tampered Title\"");
        Files.writeString(task2File, tamperedContent, StandardCharsets.UTF_8);

        TaskBoardFileMirrorVerificationReport report = mirrorHandler.verify();
        assertTrue(report.hasDrift(), "Drift must be detected");
        assertTrue(report.drifts().stream().anyMatch(d -> d.recordId().equals(task1.taskId().toString())
                && d.driftType() == TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR));
        assertTrue(report.drifts().stream().anyMatch(d -> d.recordId().equals(task2.taskId().toString())));

        TaskBoardFileMirrorReconcileResult reconcileResult = mirrorHandler.reconcile();
        assertTrue(reconcileResult.driftsResolved() >= 2, "Reconcile must update both drifted records");

        TaskBoardFileMirrorVerificationReport cleanReport = mirrorHandler.verify();
        assertFalse(cleanReport.hasDrift(), "Mirror must be completely in-sync after reconcile");
        assertTrue(cleanReport.drifts().isEmpty(), "No drifts remaining");

        JsonNode restoredTask2Json = objectMapper.readTree(Files.readString(task2File, StandardCharsets.UTF_8));
        assertEquals("Task 2", restoredTask2Json.get("payload").get("title").asText(),
                "PostgreSQL must win on disagreement");
    }

    @Test
    void tornFileQuarantined() throws IOException {
        Path taskDir = mirrorRoot.resolve("qa/task");
        Files.createDirectories(taskDir);

        String corruptId = UUID.randomUUID().toString();
        Path corruptFile = taskDir.resolve(corruptId + ".json");
        Files.writeString(corruptFile, "{ torn json payload without closing brace...", StandardCharsets.UTF_8);

        TaskBoardFileMirrorVerificationReport report = mirrorHandler.verify();
        assertTrue(report.hasDrift());
        assertTrue(report.drifts().stream().anyMatch(d -> d.recordId().equals(corruptId)
                && d.driftType() == TaskBoardFileMirrorDriftType.CORRUPT_OR_TORN));

        assertFalse(Files.exists(corruptFile), "Torn file must be removed from mirror directory");

        Path quarantineDir = taskDir.resolve("quarantine");
        assertTrue(Files.exists(quarantineDir), "Quarantine directory must exist");
        try (Stream<Path> quarantined = Files.list(quarantineDir)) {
            assertTrue(quarantined.anyMatch(p -> p.getFileName().toString().startsWith(corruptId)),
                    "Torn file must be moved to quarantine");
        }

        assertFalse(mirrorHandler.recoveryReports().isEmpty(), "Recovery report must log quarantined file");
    }

    @Test
    void neverReadMirrorInNormalRequestPaths() throws IOException {
        String projectKey = "proj-noread-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        mutations.createProject(administrator, new TaskBoardProjectCreateRequest(projectKey, "No Read Project",
                "No read project", TaskBoardProjectStatus.ACTIVE, TaskBoardAudience.PUBLIC, 0));

        TaskBoardTask task = mutations.createTask(administrator, new TaskBoardTaskCreateRequest(projectKey, "noread-task",
                "Authoritative Postgres Task", "Authoritative Postgres Task", "Instructions", "Summary",
                TaskBoardTaskPriority.NORMAL, TaskBoardAudience.PUBLIC, 1, false, false, "test",
                "https://example.invalid/source", 0));

        TaskBoardExecutionLeaseRequest leaseRequest = new TaskBoardExecutionLeaseRequest(task.taskId(),
                UUID.randomUUID(), UUID.randomUUID(), TaskBoardTaskFingerprint.compute(task), Duration.ofMinutes(5));
        TaskBoardExecutionLeaseData lease = checkpointHandler.acquireLease(administrator, leaseRequest);

        UUID checkpointId = UUID.randomUUID();
        TaskBoardExecutionCheckpointPayloadData payload = samplePayload("Instructions", "step");
        checkpointHandler.appendCheckpoint(administrator, new TaskBoardExecutionCheckpointRequest(
                checkpointId, lease,
                new TaskBoardExecutionSourceData("TavallStudios/tavall-qa", "working/qa-mirror", "2".repeat(40),
                        payload.diff().sha256()),
                payload, "codex", TaskBoardExecutionStatus.COMPLETED));

        Path qaDir = mirrorRoot.resolve("qa");
        if (Files.exists(qaDir)) {
            try (Stream<Path> stream = Files.walk(qaDir)) {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
        assertFalse(Files.exists(qaDir), "Mirror directory completely deleted");

        Optional<TaskBoardTask> loadedTask = tasks.findTask(task.taskId());
        assertTrue(loadedTask.isPresent(), "findTask must read directly from PostgreSQL");
        assertEquals("Authoritative Postgres Task", loadedTask.orElseThrow().title());

        List<TaskBoardTask> projectTasks = tasks.findTasks(projectKey);
        assertEquals(1, projectTasks.size(), "findTasks must read directly from PostgreSQL");

        Optional<TaskBoardExecutionLeaseData> loadedLease = checkpointData.currentLease(task.taskId());
        assertTrue(loadedLease.isPresent(), "currentLease must read directly from PostgreSQL");

        List<TaskBoardExecutionCheckpointData> loadedCheckpoints = checkpointData.checkpoints(task.taskId(), lease.runId(), 10);
        assertEquals(1, loadedCheckpoints.size(), "checkpoints must read directly from PostgreSQL");
    }

    private static TaskBoardExecutionCheckpointPayloadData samplePayload(String instructions, String nextAction) {
        return new TaskBoardExecutionCheckpointPayloadData(1, instructions, List.of("Preserve task identity"),
                TaskBoardExecutionDiffData.inline(""), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of("tavall-agent-builder"), List.of(), List.of(), List.of(), List.of("REPOSITORY_READ"), List.of(), nextAction);
    }
}
