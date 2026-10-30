package org.tavall.qa.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.entity.IPostgresEntityStore;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.filesystem.api.FilesystemAtomicOperations;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDrift;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorDriftType;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorRecord;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorRecordType;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorReconcileResult;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorSettings;
import org.tavall.qa.domain.mirror.TaskBoardFileMirrorVerificationReport;
import org.tavall.qa.persistence.entity.TaskBoardExecutionCheckpointEntity;
import org.tavall.qa.persistence.entity.TaskBoardExecutionLeaseEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * File redundancy mirror handler for PostgreSQL-authoritative Task Board state.
 * Writes typed versioned records after PostgreSQL commits, never serves normal request paths,
 * provides explicit verify/reconcile operations, and quarantines torn or invalid files.
 */
@DelegatesTo(ITaskBoardFileMirrorHandler.class)
public final class TaskBoardFileMirrorHandler extends GenerationOwnedDependencyAccess
        implements ITaskBoardFileMirrorHandler,
        DependencyAccess<TaskBoardFileMirrorSettings, FilesystemAtomicOperations, ObjectMapper, IPostgresDatabase, Clock> {

    public static final String OWNER_IDENTITY = "qa";
    public static final int SCHEMA_VERSION = 1;

    private static final System.Logger LOGGER = System.getLogger(TaskBoardFileMirrorHandler.class.getName());

    private final ConcurrentLinkedQueue<String> quarantineReports = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<TaskBoardFileMirrorDrift> recordedDrifts = new ConcurrentLinkedQueue<>();
    private volatile boolean simulateWriteFailure = false;

    public void setSimulateWriteFailure(boolean simulateWriteFailure) {
        this.simulateWriteFailure = simulateWriteFailure;
    }

    public boolean isSimulateWriteFailure() {
        return simulateWriteFailure;
    }

    @Override
    public Path mirrorRoot() {
        return getSettings().mirrorRoot();
    }

    @Override
    public void mirrorTask(TaskBoardTask task) {
        Objects.requireNonNull(task, "task");
        String id = task.taskId().toString();
        try {
            long revision = taskRevision(task);
            ObjectNode payload = encodeTask(task);
            writeRecordUnlocked(TaskBoardFileMirrorRecordType.TASK.directoryName(), id, revision, task.updatedAt(), payload);
        } catch (Exception exception) {
            String message = "Mirror write failed for task " + id + ": " + exception.getMessage();
            LOGGER.log(System.Logger.Level.WARNING, message);
            recordDrift(TaskBoardFileMirrorRecordType.TASK.directoryName(), id,
                    TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, message);
        }
    }

    @Override
    public void mirrorExecutionLease(TaskBoardExecutionLeaseData lease) {
        Objects.requireNonNull(lease, "lease");
        String id = lease.taskId().toString();
        try {
            ObjectNode payload = encodeLease(lease);
            writeRecordUnlocked(TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), id, lease.revision(),
                    lease.expiresAt(), payload);
        } catch (Exception exception) {
            String message = "Mirror write failed for execution lease " + id + ": " + exception.getMessage();
            LOGGER.log(System.Logger.Level.WARNING, message);
            recordDrift(TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), id,
                    TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, message);
        }
    }

    @Override
    public void mirrorExecutionCheckpoint(TaskBoardExecutionCheckpointData checkpoint) {
        Objects.requireNonNull(checkpoint, "checkpoint");
        String id = checkpoint.checkpointId().toString();
        try {
            ObjectNode payload = encodeCheckpoint(checkpoint);
            writeRecordUnlocked(TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), id, checkpoint.sequence(),
                    checkpoint.recordedAt(), payload);
        } catch (Exception exception) {
            String message = "Mirror write failed for checkpoint " + id + ": " + exception.getMessage();
            LOGGER.log(System.Logger.Level.WARNING, message);
            recordDrift(TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), id,
                    TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, message);
        }
    }

    @Override
    public TaskBoardFileMirrorVerificationReport verify() {
        Instant now = getClock().instant();
        List<TaskBoardFileMirrorDrift> drifts = new ArrayList<>();
        IPostgresEntityStore entities = requireEntities();

        // 1. Verify tasks
        Map<String, TaskBoardTask> postgresTasks = new HashMap<>();
        for (TaskBoardTaskEntity entity : entities.findNamed(TaskBoardTaskEntity.class, TaskBoardTaskEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardTask domain = entity.toDomain();
            postgresTasks.put(domain.taskId().toString(), domain);
        }
        Map<String, TaskBoardTask> mirrorTasks = scanMirrorTasks(drifts);
        for (Map.Entry<String, TaskBoardTask> entry : postgresTasks.entrySet()) {
            String id = entry.getKey();
            TaskBoardTask postgresTask = entry.getValue();
            TaskBoardTask mirrorTask = mirrorTasks.get(id);
            if (mirrorTask == null) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.TASK.directoryName(), id,
                        TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, "Task missing from mirror"));
            } else if (!matchesTask(postgresTask, mirrorTask)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.TASK.directoryName(), id,
                        TaskBoardFileMirrorDriftType.CONTENT_MISMATCH, "Task content differs from PostgreSQL authority"));
            }
        }
        for (String mirrorId : mirrorTasks.keySet()) {
            if (!postgresTasks.containsKey(mirrorId)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.TASK.directoryName(), mirrorId,
                        TaskBoardFileMirrorDriftType.MISSING_IN_POSTGRES, "Task mirror file has no matching PostgreSQL authority"));
            }
        }

        // 2. Verify execution leases
        Map<String, TaskBoardExecutionLeaseData> postgresLeases = new HashMap<>();
        for (TaskBoardExecutionLeaseEntity entity : entities.findNamed(TaskBoardExecutionLeaseEntity.class,
                TaskBoardExecutionLeaseEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardExecutionLeaseData domain = entity.toDomain();
            postgresLeases.put(domain.taskId().toString(), domain);
        }
        Map<String, TaskBoardExecutionLeaseData> mirrorLeases = scanMirrorLeases(drifts);
        for (Map.Entry<String, TaskBoardExecutionLeaseData> entry : postgresLeases.entrySet()) {
            String id = entry.getKey();
            TaskBoardExecutionLeaseData postgresLease = entry.getValue();
            TaskBoardExecutionLeaseData mirrorLease = mirrorLeases.get(id);
            if (mirrorLease == null) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), id,
                        TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, "Execution lease missing from mirror"));
            } else if (!matchesLease(postgresLease, mirrorLease)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), id,
                        TaskBoardFileMirrorDriftType.CONTENT_MISMATCH, "Execution lease differs from PostgreSQL authority"));
            }
        }
        for (String mirrorId : mirrorLeases.keySet()) {
            if (!postgresLeases.containsKey(mirrorId)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), mirrorId,
                        TaskBoardFileMirrorDriftType.MISSING_IN_POSTGRES, "Execution lease mirror file has no matching PostgreSQL authority"));
            }
        }

        // 3. Verify checkpoints
        Map<String, TaskBoardExecutionCheckpointData> postgresCheckpoints = new HashMap<>();
        for (TaskBoardExecutionCheckpointEntity entity : entities.findNamed(TaskBoardExecutionCheckpointEntity.class,
                TaskBoardExecutionCheckpointEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardExecutionCheckpointData domain = entity.toDomain();
            postgresCheckpoints.put(domain.checkpointId().toString(), domain);
        }
        Map<String, TaskBoardExecutionCheckpointData> mirrorCheckpoints = scanMirrorCheckpoints(drifts);
        for (Map.Entry<String, TaskBoardExecutionCheckpointData> entry : postgresCheckpoints.entrySet()) {
            String id = entry.getKey();
            TaskBoardExecutionCheckpointData postgresCheckpoint = entry.getValue();
            TaskBoardExecutionCheckpointData mirrorCheckpoint = mirrorCheckpoints.get(id);
            if (mirrorCheckpoint == null) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), id,
                        TaskBoardFileMirrorDriftType.MISSING_IN_MIRROR, "Checkpoint missing from mirror"));
            } else if (!matchesCheckpoint(postgresCheckpoint, mirrorCheckpoint)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), id,
                        TaskBoardFileMirrorDriftType.CONTENT_MISMATCH, "Checkpoint differs from PostgreSQL authority"));
            }
        }
        for (String mirrorId : mirrorCheckpoints.keySet()) {
            if (!postgresCheckpoints.containsKey(mirrorId)) {
                drifts.add(new TaskBoardFileMirrorDrift(TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), mirrorId,
                        TaskBoardFileMirrorDriftType.MISSING_IN_POSTGRES, "Checkpoint mirror file has no matching PostgreSQL authority"));
            }
        }

        // 4. Append outstanding recorded write-failure drifts if not already represented
        for (TaskBoardFileMirrorDrift recorded : recordedDrifts) {
            boolean alreadyReported = drifts.stream().anyMatch(d ->
                    d.recordType().equals(recorded.recordType()) && d.recordId().equals(recorded.recordId()));
            if (!alreadyReported) {
                drifts.add(recorded);
            }
        }

        return new TaskBoardFileMirrorVerificationReport(now, drifts);
    }

    @Override
    public TaskBoardFileMirrorReconcileResult reconcile() {
        Instant now = getClock().instant();
        IPostgresEntityStore entities = requireEntities();
        TaskBoardFileMirrorVerificationReport preReport = verify();

        int tasksMirrored = 0;
        int leasesMirrored = 0;
        int checkpointsMirrored = 0;
        int quarantinedFiles = 0;

        // Reconcile tasks from PostgreSQL
        for (TaskBoardTaskEntity entity : entities.findNamed(TaskBoardTaskEntity.class, TaskBoardTaskEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardTask domain = entity.toDomain();
            mirrorTask(domain);
            tasksMirrored++;
        }

        // Reconcile execution leases from PostgreSQL
        for (TaskBoardExecutionLeaseEntity entity : entities.findNamed(TaskBoardExecutionLeaseEntity.class,
                TaskBoardExecutionLeaseEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardExecutionLeaseData domain = entity.toDomain();
            mirrorExecutionLease(domain);
            leasesMirrored++;
        }

        // Reconcile checkpoints from PostgreSQL
        for (TaskBoardExecutionCheckpointEntity entity : entities.findNamed(TaskBoardExecutionCheckpointEntity.class,
                TaskBoardExecutionCheckpointEntity.FIND_ALL, Map.of(), 0)) {
            TaskBoardExecutionCheckpointData domain = entity.toDomain();
            mirrorExecutionCheckpoint(domain);
            checkpointsMirrored++;
        }

        // Quarantine any orphaned files that existed only in mirror (PostgreSQL wins)
        for (TaskBoardFileMirrorDrift drift : preReport.drifts()) {
            if (drift.driftType() == TaskBoardFileMirrorDriftType.MISSING_IN_POSTGRES) {
                Path orphaned = resolveRecordFile(drift.recordType(), drift.recordId());
                if (Files.exists(orphaned, LinkOption.NOFOLLOW_LINKS)) {
                    quarantine(orphaned, drift.recordType(), drift.recordId(), "ORPHANED_NOT_IN_POSTGRESQL");
                    quarantinedFiles++;
                }
            } else if (drift.driftType() == TaskBoardFileMirrorDriftType.CORRUPT_OR_TORN) {
                quarantinedFiles++;
            }
        }

        int driftsResolved = preReport.driftCount();
        recordedDrifts.clear();

        return new TaskBoardFileMirrorReconcileResult(
                now,
                tasksMirrored,
                leasesMirrored,
                checkpointsMirrored,
                driftsResolved,
                quarantinedFiles
        );
    }

    @Override
    public void recordDrift(String recordType, String recordId, TaskBoardFileMirrorDriftType driftType, String details) {
        recordedDrifts.add(new TaskBoardFileMirrorDrift(
                Objects.requireNonNull(recordType, "recordType"),
                Objects.requireNonNull(recordId, "recordId"),
                Objects.requireNonNull(driftType, "driftType"),
                Objects.requireNonNull(details, "details")
        ));
    }

    @Override
    public List<String> recoveryReports() {
        return List.copyOf(quarantineReports);
    }

    @Override
    public List<TaskBoardFileMirrorDrift> recordedDrifts() {
        return List.copyOf(recordedDrifts);
    }

    private void writeRecordUnlocked(String recordType, String id, long revision, Instant updatedAt, ObjectNode payload)
            throws IOException {
        if (simulateWriteFailure) {
            throw new IOException("Simulated mirror write failure for " + recordType + "/" + id);
        }
        ObjectMapper mapper = getObjectMapper();
        ObjectNode record = mapper.createObjectNode();
        record.put("schemaVersion", SCHEMA_VERSION);
        record.put("ownerIdentity", OWNER_IDENTITY);
        record.put("recordType", recordType);
        record.put("recordId", id);
        record.put("revision", revision);
        record.put("updatedAt", updatedAt.truncatedTo(ChronoUnit.MICROS).toString());
        record.set("payload", payload);

        byte[] canonicalRecord = mapper.writeValueAsBytes(record);
        String digest = getFilesystemAtomicOperations().sha256(canonicalRecord);

        ObjectNode envelope = mapper.createObjectNode();
        envelope.set("record", record);
        envelope.put("sha256", digest);

        byte[] bytes = mapper.writeValueAsBytes(envelope);

        getFilesystemAtomicOperations().createContainedDirectories(getMirrorRoot(), "qa/" + recordType);
        Path target = resolveRecordFile(recordType, id);
        getFilesystemAtomicOperations().replaceAtomically(target, bytes);
    }

    private Map<String, TaskBoardTask> scanMirrorTasks(List<TaskBoardFileMirrorDrift> drifts) {
        Map<String, TaskBoardTask> results = new HashMap<>();
        Path directory = getMirrorRoot().resolve("qa/" + TaskBoardFileMirrorRecordType.TASK.directoryName());
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return results;
        }
        for (Path file : listRecordFiles(directory, TaskBoardFileMirrorRecordType.TASK.directoryName())) {
            String id = extractRecordId(file);
            Optional<TaskBoardTask> task = readTaskRecord(file, id, drifts);
            task.ifPresent(value -> results.put(id, value));
        }
        return results;
    }

    private Map<String, TaskBoardExecutionLeaseData> scanMirrorLeases(List<TaskBoardFileMirrorDrift> drifts) {
        Map<String, TaskBoardExecutionLeaseData> results = new HashMap<>();
        Path directory = getMirrorRoot().resolve("qa/" + TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName());
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return results;
        }
        for (Path file : listRecordFiles(directory, TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName())) {
            String id = extractRecordId(file);
            Optional<TaskBoardExecutionLeaseData> lease = readLeaseRecord(file, id, drifts);
            lease.ifPresent(value -> results.put(id, value));
        }
        return results;
    }

    private Map<String, TaskBoardExecutionCheckpointData> scanMirrorCheckpoints(List<TaskBoardFileMirrorDrift> drifts) {
        Map<String, TaskBoardExecutionCheckpointData> results = new HashMap<>();
        Path directory = getMirrorRoot().resolve("qa/" + TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName());
        if (!Files.exists(directory, LinkOption.NOFOLLOW_LINKS)) {
            return results;
        }
        for (Path file : listRecordFiles(directory, TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName())) {
            String id = extractRecordId(file);
            Optional<TaskBoardExecutionCheckpointData> checkpoint = readCheckpointRecord(file, id, drifts);
            checkpoint.ifPresent(value -> results.put(id, value));
        }
        return results;
    }

    private List<Path> listRecordFiles(Path directory, String recordType) {
        try (Stream<Path> paths = Files.list(directory)) {
            return paths.filter(path -> {
                String name = path.getFileName().toString();
                return !name.equals("quarantine") && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
            }).sorted(Comparator.comparing(path -> path.getFileName().toString())).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not list mirror directory: " + directory, exception);
        }
    }

    private String extractRecordId(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    private Optional<JsonNode> readValidRecordNode(Path file, String recordType, String expectedId,
            List<TaskBoardFileMirrorDrift> drifts) {
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            JsonNode envelope = getObjectMapper().readTree(bytes);
            if (envelope == null || !envelope.isObject() || !envelope.path("record").isObject()) {
                throw new InvalidRecord("RECORD_SHAPE_INVALID");
            }
            JsonNode recordNode = envelope.path("record");
            String expectedDigest = envelope.path("sha256").asText("");
            byte[] canonical = getObjectMapper().writeValueAsBytes(recordNode);
            String actualDigest = getFilesystemAtomicOperations().sha256(canonical);
            if (expectedDigest.isBlank() || !expectedDigest.equals(actualDigest)) {
                throw new InvalidRecord("RECORD_DIGEST_INVALID");
            }
            int schemaVersion = recordNode.path("schemaVersion").asInt(-1);
            if (schemaVersion != SCHEMA_VERSION) {
                throw new InvalidRecord("SCHEMA_VERSION_UNSUPPORTED:" + schemaVersion);
            }
            String owner = recordNode.path("ownerIdentity").asText("");
            if (!OWNER_IDENTITY.equals(owner)) {
                throw new InvalidRecord("OWNER_IDENTITY_MISMATCH:" + owner);
            }
            String id = recordNode.path("recordId").asText("");
            if (!expectedId.equals(id)) {
                throw new InvalidRecord("RECORD_KEY_MISMATCH:" + id);
            }
            return Optional.of(recordNode);
        } catch (InvalidRecord invalid) {
            quarantine(file, recordType, expectedId, invalid.reason);
            drifts.add(new TaskBoardFileMirrorDrift(recordType, expectedId,
                    TaskBoardFileMirrorDriftType.CORRUPT_OR_TORN, invalid.reason));
            return Optional.empty();
        } catch (Exception ex) {
            quarantine(file, recordType, expectedId, "RECORD_TORN_OR_SCHEMA_INVALID");
            drifts.add(new TaskBoardFileMirrorDrift(recordType, expectedId,
                    TaskBoardFileMirrorDriftType.CORRUPT_OR_TORN, "RECORD_TORN_OR_SCHEMA_INVALID: " + ex.getMessage()));
            return Optional.empty();
        }
    }

    private Optional<TaskBoardTask> readTaskRecord(Path file, String expectedId, List<TaskBoardFileMirrorDrift> drifts) {
        return readValidRecordNode(file, TaskBoardFileMirrorRecordType.TASK.directoryName(), expectedId, drifts)
                .map(node -> decodeTask(node.path("payload")));
    }

    private Optional<TaskBoardExecutionLeaseData> readLeaseRecord(Path file, String expectedId,
            List<TaskBoardFileMirrorDrift> drifts) {
        return readValidRecordNode(file, TaskBoardFileMirrorRecordType.EXECUTION_LEASE.directoryName(), expectedId, drifts)
                .map(node -> decodeLease(node.path("payload")));
    }

    private Optional<TaskBoardExecutionCheckpointData> readCheckpointRecord(Path file, String expectedId,
            List<TaskBoardFileMirrorDrift> drifts) {
        return readValidRecordNode(file, TaskBoardFileMirrorRecordType.CHECKPOINT.directoryName(), expectedId, drifts)
                .map(node -> decodeCheckpoint(node.path("payload")));
    }

    private void quarantine(Path source, String recordType, String id, String reason) {
        try {
            Path quarantineDirectory = getFilesystemAtomicOperations().createContainedDirectories(
                    getMirrorRoot(), "qa/" + recordType + "/quarantine");
            Path target = getFilesystemAtomicOperations().resolveContained(getMirrorRoot(),
                    "qa/" + recordType + "/quarantine/" + id + "." + UUID.randomUUID() + ".json");
            if (Files.exists(source, LinkOption.NOFOLLOW_LINKS)) {
                getFilesystemAtomicOperations().moveAtomically(source, target);
            }
            String report = "Quarantined " + recordType + " record " + id + " as " + reason
                    + " under " + quarantineDirectory.getFileName();
            quarantineReports.add(report);
            LOGGER.log(System.Logger.Level.WARNING, report);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not quarantine invalid " + recordType + " mirror state", exception);
        }
    }

    private Path resolveRecordFile(String recordType, String id) {
        try {
            String filename = id.endsWith(".json") ? id : id + ".json";
            return getFilesystemAtomicOperations().resolveContained(getMirrorRoot(),
                    "qa/" + recordType + "/" + filename);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not resolve mirror record path for " + recordType + "/" + id, exception);
        }
    }

    private ObjectNode encodeTask(TaskBoardTask task) {
        ObjectMapper mapper = getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("taskId", task.taskId().toString());
        node.put("projectKey", task.projectKey());
        node.put("title", task.title());
        node.put("description", task.description());
        node.put("status", task.status().name());
        node.put("qaStatus", task.qaStatus().name());
        node.put("priority", task.priority().name());
        node.put("audience", task.audience().name());
        node.put("sortOrder", task.sortOrder());
        node.put("qaQcEligible", task.qaQcEligible());
        node.put("createdAt", task.createdAt().truncatedTo(ChronoUnit.MICROS).toString());
        node.put("updatedAt", task.updatedAt().truncatedTo(ChronoUnit.MICROS).toString());
        return node;
    }

    private TaskBoardTask decodeTask(JsonNode node) {
        return new TaskBoardTask(
                UUID.fromString(node.path("taskId").asText()),
                node.path("projectKey").asText(),
                node.path("title").asText(),
                node.path("description").asText(),
                TaskBoardTaskStatus.valueOf(node.path("status").asText()),
                TaskBoardQaStatus.valueOf(node.path("qaStatus").asText()),
                TaskBoardTaskPriority.valueOf(node.path("priority").asText()),
                TaskBoardAudience.valueOf(node.path("audience").asText(TaskBoardAudience.PUBLIC.name())),
                node.path("sortOrder").asInt(0),
                node.path("qaQcEligible").asBoolean(false),
                Instant.parse(node.path("createdAt").asText()),
                Instant.parse(node.path("updatedAt").asText())
        );
    }

    private ObjectNode encodeLease(TaskBoardExecutionLeaseData lease) {
        ObjectMapper mapper = getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("taskId", lease.taskId().toString());
        node.put("runId", lease.runId().toString());
        node.put("leaseId", lease.leaseId().toString());
        node.put("ownerAccountId", lease.ownerAccountId().toString());
        node.put("revision", lease.revision());
        node.put("checkpointSequence", lease.checkpointSequence());
        node.put("taskFingerprint", lease.taskFingerprint());
        node.put("expiresAt", lease.expiresAt().truncatedTo(ChronoUnit.MICROS).toString());
        return node;
    }

    private TaskBoardExecutionLeaseData decodeLease(JsonNode node) {
        return new TaskBoardExecutionLeaseData(
                UUID.fromString(node.path("taskId").asText()),
                UUID.fromString(node.path("runId").asText()),
                UUID.fromString(node.path("leaseId").asText()),
                UUID.fromString(node.path("ownerAccountId").asText()),
                node.path("revision").asLong(),
                node.path("checkpointSequence").asLong(),
                node.path("taskFingerprint").asText(),
                Instant.parse(node.path("expiresAt").asText())
        );
    }

    private ObjectNode encodeCheckpoint(TaskBoardExecutionCheckpointData checkpoint) {
        ObjectMapper mapper = getObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("checkpointId", checkpoint.checkpointId().toString());
        node.put("taskId", checkpoint.taskId().toString());
        node.put("runId", checkpoint.runId().toString());
        node.put("leaseId", checkpoint.leaseId().toString());
        node.put("sequence", checkpoint.sequence());
        node.put("taskFingerprint", checkpoint.taskFingerprint());

        ObjectNode source = node.putObject("source");
        source.put("repository", checkpoint.source().repository());
        source.put("branch", checkpoint.source().branch());
        source.put("commitSha", checkpoint.source().commitSha());
        source.put("diffSha256", checkpoint.source().diffSha256());

        node.put("payloadSha256", checkpoint.payloadSha256());
        node.put("providerId", checkpoint.providerId());
        node.put("status", checkpoint.status().name());
        node.put("recordedByAccountId", checkpoint.recordedByAccountId().toString());
        node.put("recordedAt", checkpoint.recordedAt().truncatedTo(ChronoUnit.MICROS).toString());
        return node;
    }

    private TaskBoardExecutionCheckpointData decodeCheckpoint(JsonNode node) {
        JsonNode sourceNode = node.path("source");
        TaskBoardExecutionSourceData source = new TaskBoardExecutionSourceData(
                sourceNode.path("repository").asText(),
                sourceNode.path("branch").asText(),
                sourceNode.path("commitSha").asText(),
                sourceNode.path("diffSha256").asText()
        );
        return new TaskBoardExecutionCheckpointData(
                UUID.fromString(node.path("checkpointId").asText()),
                UUID.fromString(node.path("taskId").asText()),
                UUID.fromString(node.path("runId").asText()),
                UUID.fromString(node.path("leaseId").asText()),
                node.path("sequence").asLong(),
                node.path("taskFingerprint").asText(),
                source,
                node.path("payloadSha256").asText(),
                node.path("providerId").asText(),
                TaskBoardExecutionStatus.valueOf(node.path("status").asText()),
                UUID.fromString(node.path("recordedByAccountId").asText()),
                Instant.parse(node.path("recordedAt").asText())
        );
    }

    private boolean matchesTask(TaskBoardTask postgres, TaskBoardTask mirror) {
        return postgres.taskId().equals(mirror.taskId())
                && postgres.projectKey().equals(mirror.projectKey())
                && postgres.title().equals(mirror.title())
                && postgres.description().equals(mirror.description())
                && postgres.status() == mirror.status()
                && postgres.qaStatus() == mirror.qaStatus()
                && postgres.priority() == mirror.priority()
                && postgres.sortOrder() == mirror.sortOrder()
                && postgres.qaQcEligible() == mirror.qaQcEligible()
                && postgres.updatedAt().truncatedTo(ChronoUnit.MICROS).equals(
                        mirror.updatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    private boolean matchesLease(TaskBoardExecutionLeaseData postgres, TaskBoardExecutionLeaseData mirror) {
        return postgres.taskId().equals(mirror.taskId())
                && postgres.runId().equals(mirror.runId())
                && postgres.leaseId().equals(mirror.leaseId())
                && postgres.ownerAccountId().equals(mirror.ownerAccountId())
                && postgres.revision() == mirror.revision()
                && postgres.checkpointSequence() == mirror.checkpointSequence()
                && postgres.taskFingerprint().equals(mirror.taskFingerprint())
                && postgres.expiresAt().truncatedTo(ChronoUnit.MICROS).equals(
                        mirror.expiresAt().truncatedTo(ChronoUnit.MICROS));
    }

    private boolean matchesCheckpoint(TaskBoardExecutionCheckpointData postgres, TaskBoardExecutionCheckpointData mirror) {
        return postgres.checkpointId().equals(mirror.checkpointId())
                && postgres.taskId().equals(mirror.taskId())
                && postgres.runId().equals(mirror.runId())
                && postgres.leaseId().equals(mirror.leaseId())
                && postgres.sequence() == mirror.sequence()
                && postgres.taskFingerprint().equals(mirror.taskFingerprint())
                && postgres.source().equals(mirror.source())
                && postgres.payloadSha256().equals(mirror.payloadSha256())
                && postgres.providerId().equals(mirror.providerId())
                && postgres.status() == mirror.status()
                && postgres.recordedByAccountId().equals(mirror.recordedByAccountId())
                && postgres.recordedAt().truncatedTo(ChronoUnit.MICROS).equals(
                        mirror.recordedAt().truncatedTo(ChronoUnit.MICROS));
    }

    private static long taskRevision(TaskBoardTask task) {
        Instant time = task.updatedAt();
        return Math.addExact(Math.multiplyExact(time.getEpochSecond(), 1_000_000L), time.getNano() / 1_000L);
    }

    private Path getMirrorRoot() {
        return getSettings().mirrorRoot();
    }

    private TaskBoardFileMirrorSettings getSettings() {
        return getInstance().taskBoardFileMirrorSettings();
    }

    private FilesystemAtomicOperations getFilesystemAtomicOperations() {
        return getInstance().filesystemAtomicOperations();
    }

    private ObjectMapper getObjectMapper() {
        return getInstance().objectMapper();
    }

    private IPostgresEntityStore requireEntities() {
        IPostgresDatabase database = getInstance().postgresDatabase();
        if (!database.isAvailable() || !database.entities().isOpen()) {
            throw new IllegalStateException("PostgreSQL entity store is not available for mirror verification/reconciliation");
        }
        return database.entities();
    }

    private Clock getClock() {
        try {
            return getInstance().clock();
        } catch (RuntimeException ignored) {
            return Clock.systemUTC();
        }
    }

    private static final class InvalidRecord extends Exception {
        private final String reason;

        private InvalidRecord(String reason) {
            super(reason);
            this.reason = reason;
        }
    }
}
