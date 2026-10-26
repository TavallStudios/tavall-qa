package org.tavall.qa.persistence;

import jakarta.persistence.LockModeType;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.entity.IPostgresEntityOperationContext;
import org.tavall.database.postgres.entity.IPostgresEntityStore;
import org.tavall.dependency.DependencyAccess;
import org.tavall.dependency.GenerationOwnedDependencyAccess;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskFingerprint;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.persistence.entity.TaskBoardAuditEntryEntity;
import org.tavall.qa.persistence.entity.TaskBoardExecutionCheckpointEntity;
import org.tavall.qa.persistence.entity.TaskBoardExecutionCheckpointPayloadEntity;
import org.tavall.qa.persistence.entity.TaskBoardExecutionLeaseEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;
import org.tavall.qa.request.execution.TaskBoardExecutionCheckpointRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionLeaseRequest;
import org.tavall.qa.request.execution.TaskBoardExecutionResumeRequest;
import org.tavall.qa.domain.execution.TaskBoardExecutionResumeResult;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Atomic task/lease/checkpoint/audit policy through Tavall Database's typed entity operation.
 * No connection, EntityManager, transaction lifecycle, local map or second task store is owned here.
 */
@DelegatesTo(ITaskBoardExecutionCheckpointDataHandler.class)
public final class TaskBoardExecutionCheckpointDataHandler extends GenerationOwnedDependencyAccess
        implements ITaskBoardExecutionCheckpointDataHandler, DependencyAccess<IPostgresDatabase> {
    @Override
    public TaskBoardExecutionLeaseData acquireLease(TaskBoardExecutionLeaseRequest request, UUID actorAccountId, Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(actorAccountId, "actorAccountId");
        Objects.requireNonNull(now, "now");
        return requireEntities().executeAtomic(entities -> acquireLease(entities, request, actorAccountId, now));
    }

    private TaskBoardExecutionLeaseData acquireLease(IPostgresEntityOperationContext entities,
            TaskBoardExecutionLeaseRequest request, UUID actorAccountId, Instant now) {
        TaskBoardTask task = lockedTask(entities, request.taskId());
        requireExecutable(task);
        String fingerprint = TaskBoardTaskFingerprint.compute(task);
        if (!fingerprint.equals(request.expectedTaskFingerprint())) throw conflict("Task changed before lease acquisition");
        Optional<TaskBoardExecutionLeaseData> previous = entities.find(TaskBoardExecutionLeaseEntity.class,
                request.taskId().toString(), LockModeType.PESSIMISTIC_WRITE).map(TaskBoardExecutionLeaseEntity::toDomain);
        if (previous.isPresent()) {
            TaskBoardExecutionLeaseData lease = previous.orElseThrow();
            if (lease.activeAt(now)) {
                if (lease.runId().equals(request.runId()) && lease.leaseId().equals(request.leaseId())
                        && lease.ownerAccountId().equals(actorAccountId) && lease.taskFingerprint().equals(fingerprint)) {
                    return lease; // Duplicate acquire does not extend an existing lease.
                }
                throw conflict("Task already has an active execution lease");
            }
            if (lease.leaseId().equals(request.leaseId())) throw conflict("An expired lease token cannot be replayed");
        }
        List<TaskBoardExecutionCheckpointEntity> history = entities.findNamed(TaskBoardExecutionCheckpointEntity.class,
                TaskBoardExecutionCheckpointEntity.FIND_BY_RUN,
                Map.of("taskId", request.taskId().toString(), "runId", request.runId().toString()), 1);
        long sequence = 0;
        if (!history.isEmpty()) {
            TaskBoardExecutionCheckpointData latest = history.getFirst().toDomain();
            if (!latest.taskFingerprint().equals(fingerprint)) throw conflict("Run belongs to an older task revision");
            sequence = latest.sequence();
        }
        TaskBoardExecutionLeaseData lease = new TaskBoardExecutionLeaseData(request.taskId(), request.runId(),
                request.leaseId(), actorAccountId, previous.map(value -> Math.addExact(value.revision(), 1)).orElse(0L),
                sequence, fingerprint, now.plus(request.duration()));
        TaskBoardExecutionLeaseData saved = entities.save(TaskBoardExecutionLeaseEntity.fromDomain(lease)).toDomain();
        audit(entities, task, actorAccountId, TaskBoardAuditEventType.EXECUTION_LEASE_ACQUIRED,
                "Execution lease acquired for run " + lease.runId(), now);
        return saved;

    }

    @Override
    public TaskBoardExecutionLeaseData renewLease(TaskBoardExecutionLeaseData expected, Duration duration,
            UUID actorAccountId, Instant now) {
        TaskBoardExecutionLeaseRequest.validateDuration(duration);
        return requireEntities().executeAtomic(entities -> {
            TaskBoardTask task = lockedTask(entities, expected.taskId());
            requireExecutable(task);
            TaskBoardExecutionLeaseData current = requireLease(entities, expected, actorAccountId, now, true);
            requireCurrentTask(task, current);
            TaskBoardExecutionLeaseData renewed = new TaskBoardExecutionLeaseData(current.taskId(), current.runId(),
                    current.leaseId(), current.ownerAccountId(), Math.addExact(current.revision(), 1),
                    current.checkpointSequence(), current.taskFingerprint(), now.plus(duration));
            TaskBoardExecutionLeaseData saved = entities.save(TaskBoardExecutionLeaseEntity.fromDomain(renewed)).toDomain();
            audit(entities, task, actorAccountId, TaskBoardAuditEventType.EXECUTION_LEASE_RENEWED,
                    "Execution lease renewed for run " + current.runId(), now);
            return saved;
        });
    }

    @Override
    public TaskBoardExecutionLeaseData releaseLease(TaskBoardExecutionLeaseData expected, UUID actorAccountId, Instant now) {
        return requireEntities().executeAtomic(entities -> {
            TaskBoardTask task = lockedTask(entities, expected.taskId());
            TaskBoardExecutionLeaseData current = requireLease(entities, expected, actorAccountId, now, false);
            if (!current.activeAt(now)) return current;
            TaskBoardExecutionLeaseData released = new TaskBoardExecutionLeaseData(current.taskId(), current.runId(),
                    current.leaseId(), current.ownerAccountId(), Math.addExact(current.revision(), 1),
                    current.checkpointSequence(), current.taskFingerprint(), now);
            TaskBoardExecutionLeaseData saved = entities.save(TaskBoardExecutionLeaseEntity.fromDomain(released)).toDomain();
            audit(entities, task, actorAccountId, TaskBoardAuditEventType.EXECUTION_LEASE_RELEASED,
                    "Execution lease released for run " + current.runId(), now);
            return saved;
        });
    }

    @Override
    public TaskBoardExecutionCheckpointData appendCheckpoint(TaskBoardExecutionCheckpointRequest request,
            UUID actorAccountId, Instant now) {
        Objects.requireNonNull(request, "request");
        return requireEntities().executeAtomic(entities -> {
            TaskBoardExecutionLeaseData expected = request.expectedLease();
            TaskBoardTask task = lockedTask(entities, expected.taskId());
            requireExecutable(task);
            TaskBoardExecutionLeaseData current = entities.find(TaskBoardExecutionLeaseEntity.class,
                    expected.taskId().toString(), LockModeType.PESSIMISTIC_WRITE)
                    .map(TaskBoardExecutionLeaseEntity::toDomain).orElseThrow(() -> conflict("Execution lease is missing"));
            if (!current.activeAt(now) || !current.leaseId().equals(expected.leaseId())
                    || !current.runId().equals(expected.runId()) || !current.ownerAccountId().equals(actorAccountId)) {
                throw conflict("Execution lease expired or changed owner");
            }
            requireCurrentTask(task, current);
            if (!request.payload().instructions().equals(task.description())) {
                throw conflict("Checkpoint instructions differ from the canonical task");
            }
            TaskBoardExecutionCheckpointPayloadEntity payload = TaskBoardExecutionCheckpointPayloadEntity.fromDomain(
                    request.checkpointId(), request.payload());
            TaskBoardExecutionCheckpointData checkpoint = new TaskBoardExecutionCheckpointData(request.checkpointId(),
                    expected.taskId(), expected.runId(), expected.leaseId(), Math.addExact(expected.checkpointSequence(), 1),
                    expected.taskFingerprint(), request.source(), payload.payloadSha256(), request.providerId(), request.status(),
                    actorAccountId, now);
            Optional<TaskBoardExecutionCheckpointData> duplicate = entities.find(TaskBoardExecutionCheckpointEntity.class,
                    request.checkpointId().toString()).map(TaskBoardExecutionCheckpointEntity::toDomain);
            if (duplicate.isPresent()) {
                TaskBoardExecutionCheckpointData stored = duplicate.orElseThrow();
                TaskBoardExecutionCheckpointData replay = new TaskBoardExecutionCheckpointData(checkpoint.checkpointId(),
                        checkpoint.taskId(), checkpoint.runId(), checkpoint.leaseId(), checkpoint.sequence(), checkpoint.taskFingerprint(),
                        checkpoint.source(), checkpoint.payloadSha256(), checkpoint.providerId(), checkpoint.status(),
                        checkpoint.recordedByAccountId(), stored.recordedAt());
                if (!stored.equals(replay)) throw conflict("Checkpoint identity already has different content");
                return stored;
            }
            if (!current.equals(expected)) throw conflict("Execution lease revision changed; reload before appending");
            TaskBoardExecutionCheckpointData saved = entities.save(TaskBoardExecutionCheckpointEntity.fromDomain(checkpoint)).toDomain();
            entities.save(payload);
            entities.save(TaskBoardExecutionLeaseEntity.fromDomain(new TaskBoardExecutionLeaseData(current.taskId(),
                    current.runId(), current.leaseId(), current.ownerAccountId(), Math.addExact(current.revision(), 1),
                    checkpoint.sequence(), current.taskFingerprint(), current.expiresAt())));
            audit(entities, task, actorAccountId, TaskBoardAuditEventType.EXECUTION_CHECKPOINT_RECORDED,
                    "Execution checkpoint " + checkpoint.sequence() + " recorded for run " + current.runId(), now);
            return saved;
        });
    }

    @Override
    public Optional<TaskBoardExecutionLeaseData> currentLease(UUID taskId) {
        return requireEntities().find(TaskBoardExecutionLeaseEntity.class, Objects.requireNonNull(taskId, "taskId").toString())
                .map(TaskBoardExecutionLeaseEntity::toDomain);
    }

    @Override
    public List<TaskBoardExecutionCheckpointData> checkpoints(UUID taskId, UUID runId, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Checkpoint history limit must be within 1..100");
        return requireEntities().findNamed(TaskBoardExecutionCheckpointEntity.class, TaskBoardExecutionCheckpointEntity.FIND_BY_RUN,
                Map.of("taskId", Objects.requireNonNull(taskId, "taskId").toString(),
                        "runId", Objects.requireNonNull(runId, "runId").toString()), limit)
                .stream().map(TaskBoardExecutionCheckpointEntity::toDomain).toList();
    }

    @Override
    public Optional<TaskBoardExecutionCheckpointPayloadData> loadPayload(UUID taskId, UUID checkpointId) {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(checkpointId, "checkpointId");
        IPostgresEntityStore entities = requireEntities();
        Optional<TaskBoardExecutionCheckpointData> checkpoint = entities.find(TaskBoardExecutionCheckpointEntity.class,
                checkpointId.toString()).map(TaskBoardExecutionCheckpointEntity::toDomain);
        if (checkpoint.isEmpty()) return Optional.empty();
        if (!checkpoint.orElseThrow().taskId().equals(taskId)) throw new SecurityException("Checkpoint belongs to another task");
        TaskBoardExecutionCheckpointPayloadEntity payload = entities.find(TaskBoardExecutionCheckpointPayloadEntity.class,
                checkpointId.toString()).orElseThrow(() -> new TaskBoardStorageUnavailableException("Checkpoint payload is missing"));
        return Optional.of(payload.toDomain(checkpoint.orElseThrow().payloadSha256()));
    }

    @Override
    public TaskBoardExecutionResumeResult resumeCheckpoint(TaskBoardExecutionResumeRequest request,
            UUID actorAccountId, Instant now) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(actorAccountId, "actorAccountId");
        Objects.requireNonNull(now, "now");
        return requireEntities().executeAtomic(entities -> {
            TaskBoardExecutionLeaseRequest leaseRequest = request.leaseRequest();
            TaskBoardTask task = lockedTask(entities, leaseRequest.taskId());
            requireExecutable(task);
            TaskBoardExecutionCheckpointData checkpoint = entities.find(TaskBoardExecutionCheckpointEntity.class,
                    request.checkpointId().toString()).map(TaskBoardExecutionCheckpointEntity::toDomain)
                    .orElseThrow(() -> conflict("Checkpoint is missing"));
            if (!checkpoint.taskId().equals(leaseRequest.taskId()) || !checkpoint.runId().equals(leaseRequest.runId())
                    || !checkpoint.taskFingerprint().equals(leaseRequest.expectedTaskFingerprint())
                    || !checkpoint.taskFingerprint().equals(TaskBoardTaskFingerprint.compute(task))
                    || !checkpoint.source().equals(request.observedSource())) {
                throw conflict("Task, source or outstanding diff changed before checkpoint resumption");
            }
            List<TaskBoardExecutionCheckpointEntity> latest = entities.findNamed(TaskBoardExecutionCheckpointEntity.class,
                    TaskBoardExecutionCheckpointEntity.FIND_BY_RUN,
                    Map.of("taskId", checkpoint.taskId().toString(), "runId", checkpoint.runId().toString()), 1);
            if (latest.isEmpty() || !latest.getFirst().toDomain().checkpointId().equals(checkpoint.checkpointId())) {
                throw conflict("Checkpoint was superseded; load the latest run context");
            }
            TaskBoardExecutionCheckpointPayloadData payload = entities.find(TaskBoardExecutionCheckpointPayloadEntity.class,
                    checkpoint.checkpointId().toString()).orElseThrow(() -> conflict("Checkpoint payload is missing"))
                    .toDomain(checkpoint.payloadSha256());
            if (!payload.instructions().equals(task.description())) throw conflict("Checkpoint task instructions changed");
            TaskBoardExecutionLeaseData lease = acquireLease(entities, leaseRequest, actorAccountId, now);
            return new TaskBoardExecutionResumeResult(lease, checkpoint, payload);
        });
    }

    private IPostgresEntityStore requireEntities() {
        IPostgresDatabase database = getInstance();
        if (!database.isAvailable() || !database.entities().isOpen()) {
            throw new TaskBoardStorageUnavailableException("Task execution checkpoint storage is unavailable");
        }
        return database.entities();
    }

    private TaskBoardTask lockedTask(IPostgresEntityOperationContext entities, UUID taskId) {
        entities.acquireAdvisoryTransactionLock(TaskBoardTaskEntity.lockKey(taskId));
        return entities.find(TaskBoardTaskEntity.class, taskId.toString(), LockModeType.PESSIMISTIC_WRITE)
                .map(TaskBoardTaskEntity::toDomain).orElseThrow(() -> conflict("Task is no longer available"));
    }

    private TaskBoardExecutionLeaseData requireLease(IPostgresEntityOperationContext entities,
            TaskBoardExecutionLeaseData expected, UUID actorAccountId, Instant now, boolean requireActive) {
        Objects.requireNonNull(expected, "expected");
        Objects.requireNonNull(actorAccountId, "actorAccountId");
        Objects.requireNonNull(now, "now");
        TaskBoardExecutionLeaseData current = entities.find(TaskBoardExecutionLeaseEntity.class,
                expected.taskId().toString(), LockModeType.PESSIMISTIC_WRITE).map(TaskBoardExecutionLeaseEntity::toDomain)
                .orElseThrow(() -> conflict("Execution lease is missing"));
        if (!current.equals(expected) || !current.ownerAccountId().equals(actorAccountId)
                || requireActive && !current.activeAt(now)) throw conflict("Execution lease expired or changed revision");
        return current;
    }

    private static void requireCurrentTask(TaskBoardTask task, TaskBoardExecutionLeaseData lease) {
        if (!TaskBoardTaskFingerprint.compute(task).equals(lease.taskFingerprint())) throw conflict("Task changed during execution");
    }

    private static void requireExecutable(TaskBoardTask task) {
        if (task.status() == TaskBoardTaskStatus.DONE || task.status() == TaskBoardTaskStatus.CANCELLED) {
            throw conflict("Completed or cancelled task must be reopened before execution");
        }
    }

    private void audit(IPostgresEntityOperationContext entities, TaskBoardTask task, UUID actor,
            TaskBoardAuditEventType event, String summary, Instant now) {
        entities.save(TaskBoardAuditEntryEntity.fromDomain(new TaskBoardAuditEntry(UUID.randomUUID(), Optional.of(task.taskId()),
                task.projectKey(), Optional.of(actor), null, event, summary, now)));
    }

    private static TaskBoardConcurrentModificationException conflict(String message) {
        return new TaskBoardConcurrentModificationException(message);
    }
}
