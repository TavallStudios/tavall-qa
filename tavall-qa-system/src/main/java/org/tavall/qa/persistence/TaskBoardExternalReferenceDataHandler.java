package org.tavall.qa.persistence;

import jakarta.persistence.LockModeType;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictResolution;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationOutcome;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSyncState;
import org.tavall.qa.persistence.entity.TaskBoardAuditEntryEntity;
import org.tavall.qa.persistence.entity.TaskBoardExternalReferenceConflictEntity;
import org.tavall.qa.persistence.entity.TaskBoardExternalReferenceEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Mapped-JPA authority for QA-owned links to externally owned work items. */
@DelegatesTo(ITaskBoardExternalReferenceDataHandler.class)
public final class TaskBoardExternalReferenceDataHandler implements ITaskBoardExternalReferenceDataHandler {
    private static final String TASK_REFERENCE_LOCK_PREFIX = "tavall-qa:external-references:task:";
    private static final String OPEN_STATUS_PARAMETER = "status";

    private final IPostgresDatabase database;
    private final Clock clock;

    public TaskBoardExternalReferenceDataHandler(IPostgresDatabase database, Clock clock) {
        this.database = Objects.requireNonNull(database, "database");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TaskBoardExternalReferenceDataHandler(IPostgresDatabase database) {
        this(database, Clock.systemUTC());
    }

    @Override
    public boolean isAvailable() {
        return database.isAvailable() && database.entities().isOpen();
    }

    @Override
    public Optional<TaskBoardExternalReference> findReference(TaskBoardExternalReferenceKey key) {
        requireAvailable();
        Objects.requireNonNull(key, "key");
        return database.entities().findOneNamed(TaskBoardExternalReferenceEntity.class,
                        TaskBoardExternalReferenceEntity.FIND_BY_KEY, keyParameters(key))
                .map(TaskBoardExternalReferenceEntity::toDomain);
    }

    @Override
    public List<TaskBoardExternalReference> findReferencesForTask(UUID taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        return database.entities().findNamed(TaskBoardExternalReferenceEntity.class,
                        TaskBoardExternalReferenceEntity.FIND_BY_TASK, Map.of("taskId", taskId.toString()), 0)
                .stream().map(TaskBoardExternalReferenceEntity::toDomain).toList();
    }

    @Override
    public List<TaskBoardExternalReferenceConflict> findOpenConflicts(Optional<UUID> taskId) {
        requireAvailable();
        Objects.requireNonNull(taskId, "taskId");
        Map<String, Object> parameters = taskId.<Map<String, Object>>map(id -> Map.of(
                OPEN_STATUS_PARAMETER, TaskBoardExternalReferenceConflictStatus.OPEN, "taskId", id.toString()))
                .orElseGet(() -> Map.of(OPEN_STATUS_PARAMETER, TaskBoardExternalReferenceConflictStatus.OPEN));
        String query = taskId.isPresent() ? TaskBoardExternalReferenceConflictEntity.FIND_OPEN_FOR_TASK
                : TaskBoardExternalReferenceConflictEntity.FIND_OPEN;
        return database.entities().findNamed(TaskBoardExternalReferenceConflictEntity.class,
                        query, parameters, 0)
                .stream().map(TaskBoardExternalReferenceConflictEntity::toDomain).toList();
    }

    @Override
    public Optional<TaskBoardExternalReferenceConflict> findConflict(UUID conflictId) {
        requireAvailable();
        Objects.requireNonNull(conflictId, "conflictId");
        return database.entities().find(TaskBoardExternalReferenceConflictEntity.class, conflictId.toString())
                .map(TaskBoardExternalReferenceConflictEntity::toDomain);
    }

    @Override
    public TaskBoardExternalReferenceMutationResult registerOrReconcile(
            TaskBoardExternalReferenceMutationRequest request,
            TaskBoardAuditEntry auditEntry
    ) {
        requireAvailable();
        Objects.requireNonNull(request, "request");
        requireAudit(auditEntry, request.taskId());
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(request.key().advisoryLockKey());
            Optional<TaskBoardExternalReferenceEntity> existingRow = findByKey(context, request.key());
            Set<UUID> lockedTasks = new HashSet<>();
            lockedTasks.add(request.taskId());
            existingRow.map(row -> UUID.fromString(row.getTaskId())).ifPresent(lockedTasks::add);
            acquireTaskReferenceLocks(context, lockedTasks);

            TaskBoardTaskEntity task = context.find(TaskBoardTaskEntity.class, request.taskId().toString(),
                            LockModeType.PESSIMISTIC_READ)
                    .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + request.taskId()));
            if (!task.toDomain().projectKey().equals(auditEntry.projectKey())) {
                throw new IllegalArgumentException("Audit project must match the referenced Task Board task");
            }

            Optional<TaskBoardExternalReferenceEntity> latest = findByKey(context, request.key());
            if (latest.isPresent() && !latest.orElseThrow().getTaskId().equals(request.taskId().toString())) {
                return conflict(context, request, auditEntry, latest, Optional.of(UUID.fromString(latest.orElseThrow().getTaskId())),
                        "External identity is already linked to another QA task");
            }

            Optional<TaskBoardExternalReferenceEntity> current = latest;
            if (current.isPresent() && isExactReplay(current.orElseThrow().toDomain(), request)) {
                return new TaskBoardExternalReferenceMutationResult(
                        Optional.of(current.orElseThrow().toDomain()), Optional.empty(), TaskBoardExternalReferenceMutationOutcome.REPLAYED);
            }

            if (current.map(TaskBoardExternalReferenceEntity::toDomain)
                    .map(reference -> reference.syncRevision() != request.expectedSyncRevision())
                    .orElse(request.expectedSyncRevision() != 0)) {
                return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                        "Expected sync revision does not match the current reference revision");
            }

            if (current.isPresent()) {
                TaskBoardExternalReference accepted = current.orElseThrow().toDomain();
                if (accepted.lastAppliedRevision().equals(request.sourceRevision())
                        && !accepted.sourceFingerprint().equalsIgnoreCase(request.sourceFingerprint())) {
                    return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                            "Source revision was replayed with a different content fingerprint");
                }
                if (request.observedAt().isBefore(accepted.lastObservedAt())) {
                    return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                            "Source observation timestamp moved backwards");
                }
                if (accepted.syncState() == TaskBoardExternalReferenceSyncState.CONFLICT) {
                    return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                            "External reference has unresolved conflicts");
                }
            }

            Optional<TaskBoardExternalReferenceEntity> otherProvider = findTaskProvider(context, request.taskId(), otherProvider(request.key().provider()));
            if (otherProvider.isPresent()
                    && contradictory(otherProvider.orElseThrow().toDomain().sourceStatus(), request.sourceStatus())) {
                return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                        "Provider source statuses disagree between active work and terminal work");
            }

            Instant now = clock.instant();
            TaskBoardExternalReference saved;
            TaskBoardExternalReferenceMutationOutcome outcome;
            if (current.isEmpty()) {
                saved = new TaskBoardExternalReference(UUID.randomUUID(), request.taskId(), request.key(),
                        request.displayKey(), request.sourceUrl(), request.sourceStatus(), request.sourceRevision(),
                        request.sourceRevision(), request.sourceCursor(), request.sourceFingerprint(),
                        TaskBoardExternalReferenceSyncState.CONNECTED, 1, request.observedAt(), request.observedAt(), now, now);
                outcome = TaskBoardExternalReferenceMutationOutcome.CREATED;
            } else {
                TaskBoardExternalReference previous = current.orElseThrow().toDomain();
                saved = copyReference(previous, request.displayKey(), request.sourceUrl(), request.sourceStatus(),
                        request.sourceRevision(), request.sourceRevision(), request.sourceCursor(), request.sourceFingerprint(),
                        TaskBoardExternalReferenceSyncState.CONNECTED, previous.syncRevision() + 1,
                        request.observedAt(), request.observedAt(), now);
                outcome = TaskBoardExternalReferenceMutationOutcome.UPDATED;
            }
            context.save(TaskBoardExternalReferenceEntity.fromDomain(saved));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditFor(auditEntry,
                    TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED,
                    "Registered or reconciled " + request.key().provider() + " external reference " + request.key().stableKey())));
            return new TaskBoardExternalReferenceMutationResult(Optional.of(saved), Optional.empty(), outcome);
        });
    }

    @Override
    public TaskBoardExternalReferenceConflict resolveConflict(
            TaskBoardExternalReferenceConflictResolutionRequest request,
            UUID actorAccountId,
            TaskBoardAuditEntry auditEntry
    ) {
        requireAvailable();
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(actorAccountId, "actorAccountId");
        Objects.requireNonNull(auditEntry, "auditEntry");
        return database.entities().executeAtomic(context -> {
            TaskBoardExternalReferenceConflictEntity conflictRow = context.find(
                            TaskBoardExternalReferenceConflictEntity.class, request.conflictId().toString(), LockModeType.PESSIMISTIC_WRITE)
                    .orElseThrow(() -> new IllegalArgumentException("External reference conflict not found: " + request.conflictId()));
            TaskBoardExternalReferenceConflict conflict = conflictRow.toDomain();
            if (conflict.status() == TaskBoardExternalReferenceConflictStatus.RESOLVED) {
                if (conflict.resolution().filter(request.resolution()::equals).isPresent()) return conflict;
                throw new TaskBoardConcurrentModificationException("External reference conflict was already resolved differently");
            }

            context.acquireAdvisoryTransactionLock(conflict.key().advisoryLockKey());
            acquireTaskReferenceLocks(context, Set.of(conflict.requestedTaskId()));
            TaskBoardTaskEntity task = context.find(TaskBoardTaskEntity.class, conflict.requestedTaskId().toString(),
                            LockModeType.PESSIMISTIC_READ)
                    .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + conflict.requestedTaskId()));
            requireAudit(auditEntry, conflict.requestedTaskId());
            if (!task.toDomain().projectKey().equals(auditEntry.projectKey())) {
                throw new IllegalArgumentException("Audit project must match the conflict task");
            }

            Optional<TaskBoardExternalReferenceEntity> currentRow = findByKey(context, conflict.key());
            Optional<TaskBoardExternalReference> current = currentRow.map(TaskBoardExternalReferenceEntity::toDomain);
            long currentRevision = current.map(TaskBoardExternalReference::syncRevision).orElse(0L);
            if (currentRevision != request.expectedSyncRevision()) {
                throw new TaskBoardConcurrentModificationException("External reference changed since the conflict was loaded");
            }
            if (current.isPresent() && !current.orElseThrow().taskId().equals(conflict.requestedTaskId())) {
                throw new TaskBoardConcurrentModificationException("External identity is now linked to another QA task");
            }

            Instant now = clock.instant();
            boolean otherOpenConflict = hasOtherOpenConflict(context, conflict);
            if (request.resolution() == TaskBoardExternalReferenceConflictResolution.APPLY_INCOMING) {
                Optional<TaskBoardExternalReferenceEntity> providerSlot = findTaskProvider(context, conflict.requestedTaskId(), conflict.key().provider());
                if (providerSlot.isPresent() && !providerSlot.orElseThrow().getSourceId().equals(conflict.key().sourceId())) {
                    throw new TaskBoardConcurrentModificationException("The QA task already has a different reference for this provider");
                }
                TaskBoardExternalReference applied = referenceFromConflict(conflict, current, now,
                        otherOpenConflict ? TaskBoardExternalReferenceSyncState.CONFLICT : TaskBoardExternalReferenceSyncState.CONNECTED);
                context.save(TaskBoardExternalReferenceEntity.fromDomain(applied));
            } else if (current.isPresent()) {
                TaskBoardExternalReference previous = current.orElseThrow();
                if (!otherOpenConflict && previous.syncState() == TaskBoardExternalReferenceSyncState.CONFLICT) {
                    TaskBoardExternalReference restored = copyReference(previous, previous.displayKey(), previous.sourceUrl(),
                            previous.sourceStatus(), previous.lastObservedRevision(), previous.lastAppliedRevision(),
                            previous.lastObservedCursor(), previous.sourceFingerprint(), TaskBoardExternalReferenceSyncState.CONNECTED,
                            previous.syncRevision() + 1, previous.lastObservedAt(), previous.lastAppliedAt(), now);
                    context.save(TaskBoardExternalReferenceEntity.fromDomain(restored));
                }
            }

            TaskBoardExternalReferenceConflict resolved = new TaskBoardExternalReferenceConflict(
                    conflict.conflictId(), conflict.key(), conflict.referenceId(), conflict.existingTaskId(),
                    conflict.requestedTaskId(), conflict.incomingDisplayKey(), conflict.incomingSourceUrl(),
                    conflict.incomingSourceStatus(), conflict.incomingSourceRevision(), conflict.incomingSourceCursor(),
                    conflict.incomingSourceFingerprint(), conflict.incomingObservedAt(), conflict.expectedSyncRevision(),
                    conflict.reason(), TaskBoardExternalReferenceConflictStatus.RESOLVED,
                    Optional.of(request.resolution()), Optional.of(actorAccountId), conflict.createdAt(), Optional.of(now));
            context.save(TaskBoardExternalReferenceConflictEntity.fromDomain(resolved));
            context.save(TaskBoardAuditEntryEntity.fromDomain(auditFor(auditEntry,
                    TaskBoardAuditEventType.EXTERNAL_REFERENCE_RESOLVED,
                    "Resolved external reference conflict " + conflict.conflictId() + " as " + request.resolution()
                            + (request.resolutionNote().isBlank() ? "" : ": " + request.resolutionNote()))));
            return resolved;
        });
    }

    private TaskBoardExternalReferenceMutationResult conflict(
            org.tavall.database.postgres.entity.IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceMutationRequest request,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardExternalReferenceEntity> current,
            Optional<UUID> existingTaskId,
            String reason
    ) {
        UUID conflictId = conflictId(request);
        Optional<TaskBoardExternalReferenceConflictEntity> alreadyQueued = context.find(
                TaskBoardExternalReferenceConflictEntity.class, conflictId.toString());
        if (alreadyQueued.isPresent()) {
            TaskBoardExternalReferenceConflict existingConflict = alreadyQueued.orElseThrow().toDomain();
            Optional<TaskBoardExternalReference> currentReference = findByKey(context, request.key())
                    .map(TaskBoardExternalReferenceEntity::toDomain);
            return new TaskBoardExternalReferenceMutationResult(currentReference,
                    Optional.of(existingConflict), TaskBoardExternalReferenceMutationOutcome.CONFLICT);
        }

        Instant now = clock.instant();
        Optional<UUID> referenceId = current.map(row -> UUID.fromString(row.getReferenceId()));
        TaskBoardExternalReferenceConflict queued = new TaskBoardExternalReferenceConflict(conflictId, request.key(), referenceId,
                existingTaskId, request.taskId(), request.displayKey(), request.sourceUrl(), request.sourceStatus(),
                request.sourceRevision(), request.sourceCursor(), request.sourceFingerprint(), request.observedAt(),
                request.expectedSyncRevision(), reason, TaskBoardExternalReferenceConflictStatus.OPEN,
                Optional.empty(), Optional.empty(), now, Optional.empty());
        Optional<TaskBoardExternalReference> resultReference = current.map(TaskBoardExternalReferenceEntity::toDomain);
        if (current.isPresent() && current.orElseThrow().getTaskId().equals(request.taskId().toString())) {
            TaskBoardExternalReference previous = current.orElseThrow().toDomain();
            boolean observationIsFresh = !request.observedAt().isBefore(previous.lastObservedAt());
            TaskBoardExternalReference conflicted = copyReference(previous, previous.displayKey(), previous.sourceUrl(),
                    previous.sourceStatus(), observationIsFresh ? request.sourceRevision() : previous.lastObservedRevision(),
                    previous.lastAppliedRevision(), observationIsFresh ? request.sourceCursor() : previous.lastObservedCursor(),
                    previous.sourceFingerprint(), TaskBoardExternalReferenceSyncState.CONFLICT, previous.syncRevision() + 1,
                    observationIsFresh ? request.observedAt() : previous.lastObservedAt(), previous.lastAppliedAt(), now);
            context.save(TaskBoardExternalReferenceEntity.fromDomain(conflicted));
            resultReference = Optional.of(conflicted);
        }
        context.save(TaskBoardExternalReferenceConflictEntity.fromDomain(queued));
        context.save(TaskBoardAuditEntryEntity.fromDomain(auditFor(auditEntry,
                TaskBoardAuditEventType.EXTERNAL_REFERENCE_CONFLICTED,
                "Queued external reference conflict for " + request.key().provider() + " " + request.key().stableKey()
                        + ": " + reason)));
        return new TaskBoardExternalReferenceMutationResult(resultReference,
                Optional.of(queued), TaskBoardExternalReferenceMutationOutcome.CONFLICT);
    }

    private static TaskBoardExternalReference referenceFromConflict(
            TaskBoardExternalReferenceConflict conflict,
            Optional<TaskBoardExternalReference> current,
            Instant now,
            TaskBoardExternalReferenceSyncState syncState
    ) {
        if (current.isEmpty()) {
            return new TaskBoardExternalReference(UUID.randomUUID(), conflict.requestedTaskId(), conflict.key(),
                    conflict.incomingDisplayKey(), conflict.incomingSourceUrl(), conflict.incomingSourceStatus(),
                    conflict.incomingSourceRevision(), conflict.incomingSourceRevision(), conflict.incomingSourceCursor(),
                    conflict.incomingSourceFingerprint(), syncState, 1,
                    conflict.incomingObservedAt(), conflict.incomingObservedAt(), now, now);
        }
        TaskBoardExternalReference previous = current.orElseThrow();
        return copyReference(previous, conflict.incomingDisplayKey(), conflict.incomingSourceUrl(),
                conflict.incomingSourceStatus(), conflict.incomingSourceRevision(), conflict.incomingSourceRevision(),
                conflict.incomingSourceCursor(), conflict.incomingSourceFingerprint(), syncState,
                previous.syncRevision() + 1, conflict.incomingObservedAt(), conflict.incomingObservedAt(), now);
    }

    private static TaskBoardExternalReference copyReference(
            TaskBoardExternalReference previous,
            String displayKey,
            String sourceUrl,
            TaskBoardExternalReferenceSourceStatus sourceStatus,
            String lastObservedRevision,
            String lastAppliedRevision,
            String lastObservedCursor,
            String sourceFingerprint,
            TaskBoardExternalReferenceSyncState syncState,
            long syncRevision,
            Instant lastObservedAt,
            Instant lastAppliedAt,
            Instant updatedAt
    ) {
        return new TaskBoardExternalReference(previous.referenceId(), previous.taskId(), previous.key(), displayKey,
                sourceUrl, sourceStatus, lastObservedRevision, lastAppliedRevision, lastObservedCursor, sourceFingerprint,
                syncState, syncRevision, lastObservedAt, lastAppliedAt, previous.createdAt(), updatedAt);
    }

    private static boolean isExactReplay(TaskBoardExternalReference current, TaskBoardExternalReferenceMutationRequest request) {
        return current.taskId().equals(request.taskId())
                && current.displayKey().equals(request.displayKey())
                && current.sourceUrl().equals(request.sourceUrl())
                && current.sourceStatus() == request.sourceStatus()
                && current.lastObservedRevision().equals(request.sourceRevision())
                && current.lastAppliedRevision().equals(request.sourceRevision())
                && current.lastObservedCursor().equals(request.sourceCursor())
                && current.sourceFingerprint().equalsIgnoreCase(request.sourceFingerprint())
                && current.syncState() == TaskBoardExternalReferenceSyncState.CONNECTED;
    }

    private static boolean contradictory(TaskBoardExternalReferenceSourceStatus existing,
            TaskBoardExternalReferenceSourceStatus incoming) {
        return existing.indicatesActiveWork() && incoming.indicatesTerminalWork()
                || existing.indicatesTerminalWork() && incoming.indicatesActiveWork();
    }

    private static TaskBoardExternalReferenceProvider otherProvider(TaskBoardExternalReferenceProvider provider) {
        return provider == TaskBoardExternalReferenceProvider.GITHUB_ISSUE
                ? TaskBoardExternalReferenceProvider.LINEAR_ISSUE : TaskBoardExternalReferenceProvider.GITHUB_ISSUE;
    }

    private static Optional<TaskBoardExternalReferenceEntity> findByKey(
            org.tavall.database.postgres.entity.IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceKey key
    ) {
        return context.findOneNamed(TaskBoardExternalReferenceEntity.class,
                TaskBoardExternalReferenceEntity.FIND_BY_KEY, keyParameters(key));
    }

    private static Optional<TaskBoardExternalReferenceEntity> findTaskProvider(
            org.tavall.database.postgres.entity.IPostgresEntityOperationContext context,
            UUID taskId,
            TaskBoardExternalReferenceProvider provider
    ) {
        return context.findOneNamed(TaskBoardExternalReferenceEntity.class,
                TaskBoardExternalReferenceEntity.FIND_BY_TASK_PROVIDER,
                Map.of("taskId", taskId.toString(), "provider", provider));
    }

    private static Map<String, Object> keyParameters(TaskBoardExternalReferenceKey key) {
        return Map.of("provider", key.provider(), "sourceNamespaceId", key.sourceNamespaceId(), "sourceId", key.sourceId());
    }

    private static void acquireTaskReferenceLocks(
            org.tavall.database.postgres.entity.IPostgresEntityOperationContext context,
            Set<UUID> taskIds
    ) {
        taskIds.stream().map(UUID::toString).sorted(Comparator.naturalOrder())
                .forEach(id -> context.acquireAdvisoryTransactionLock(TASK_REFERENCE_LOCK_PREFIX + id));
    }

    private static UUID conflictId(TaskBoardExternalReferenceMutationRequest request) {
        String fingerprint = request.key().stableKey() + "|" + request.taskId() + "|"
                + request.sourceRevision() + "|" + request.sourceFingerprint() + "|"
                + request.sourceStatus();
        return UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasOtherOpenConflict(
            org.tavall.database.postgres.entity.IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceConflict conflict
    ) {
        return context.findNamed(TaskBoardExternalReferenceConflictEntity.class,
                        TaskBoardExternalReferenceConflictEntity.FIND_OPEN_BY_KEY,
                        Map.of("provider", conflict.key().provider(),
                                "sourceNamespaceId", conflict.key().sourceNamespaceId(),
                                "sourceId", conflict.key().sourceId(), OPEN_STATUS_PARAMETER,
                                TaskBoardExternalReferenceConflictStatus.OPEN), 0)
                .stream().anyMatch(row -> !row.getConflictId().equals(conflict.conflictId().toString()));
    }

    private static TaskBoardAuditEntry auditFor(TaskBoardAuditEntry auditEntry, TaskBoardAuditEventType eventType, String summary) {
        return new TaskBoardAuditEntry(auditEntry.eventId(), auditEntry.taskId(), auditEntry.projectKey(),
                auditEntry.actorAccountId(), auditEntry.actorMinecraftUuid(), eventType, summary, auditEntry.occurredAt());
    }

    private static void requireAudit(TaskBoardAuditEntry auditEntry, UUID taskId) {
        Objects.requireNonNull(auditEntry, "auditEntry");
        if (auditEntry.taskId().filter(taskId::equals).isEmpty()) {
            throw new IllegalArgumentException("External reference audit must identify its QA task");
        }
    }

    private void requireAvailable() {
        if (!isAvailable()) throw new TaskBoardStorageUnavailableException("QA Task Board storage is unavailable");
    }
}
