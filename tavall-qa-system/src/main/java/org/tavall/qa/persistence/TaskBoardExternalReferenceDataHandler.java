package org.tavall.qa.persistence;

import jakarta.persistence.LockModeType;
import org.tavall.database.postgres.IPostgresDatabase;
import org.tavall.database.postgres.entity.IPostgresEntityOperationContext;
import org.tavall.dependency.annotations.DelegatesTo;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;
import org.tavall.qa.domain.TaskBoardConcurrentModificationException;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictResolution;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceIngestResult;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationOutcome;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceMutationResult;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSyncState;
import org.tavall.qa.persistence.entity.TaskBoardAuditEntryEntity;
import org.tavall.qa.persistence.entity.TaskBoardExternalReferenceConflictEntity;
import org.tavall.qa.persistence.entity.TaskBoardExternalReferenceAliasEntity;
import org.tavall.qa.persistence.entity.TaskBoardExternalReferenceEntity;
import org.tavall.qa.persistence.entity.TaskBoardTaskEntity;
import org.tavall.qa.request.external.TaskBoardExternalReferenceConflictResolutionRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceMutationRequest;
import org.tavall.qa.request.external.TaskBoardExternalReferenceIngestRequest;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Mapped-JPA authority for QA-owned links to externally owned work items. */
@DelegatesTo(ITaskBoardExternalReferenceDataHandler.class)
public final class TaskBoardExternalReferenceDataHandler implements ITaskBoardExternalReferenceDataHandler {
    private static final String IDENTITY_CONFLICT = "Ambiguous external identity: ";
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
    public Optional<TaskBoardExternalReference> findGitHubReference(String alias) {
        requireAvailable();
        String key = requireGitHubAlias(alias);
        return database.entities().find(TaskBoardExternalReferenceAliasEntity.class, key)
                .flatMap(row -> database.entities().find(TaskBoardExternalReferenceEntity.class, row.referenceId()))
                .map(TaskBoardExternalReferenceEntity::toDomain);
    }

    @Override
    public List<String> findGitHubAliases(TaskBoardExternalReferenceKey key) {
        return findReference(key).map(reference -> database.entities().findNamed(
                        TaskBoardExternalReferenceAliasEntity.class, TaskBoardExternalReferenceAliasEntity.FIND_BY_REFERENCE,
                        Map.of("referenceId", reference.referenceId().toString()), 0)
                .stream().map(TaskBoardExternalReferenceAliasEntity::aliasKey).toList()).orElseGet(List::of);
    }

    @Override
    public List<TaskBoardExternalReferenceConflict> findConflictHistory(UUID taskId) {
        requireAvailable();
        return database.entities().findNamed(TaskBoardExternalReferenceConflictEntity.class,
                        TaskBoardExternalReferenceConflictEntity.FIND_HISTORY_FOR_TASK,
                        Map.of("taskId", taskId.toString()), 0)
                .stream().map(TaskBoardExternalReferenceConflictEntity::toDomain).toList();
    }

    @Override
    public TaskBoardExternalReferenceIngestResult ingest(
            TaskBoardExternalReferenceIngestRequest request, TaskBoardAuditEntry actor) {
        requireAvailable();
        String alias = requireGitHubAlias(request.github().sourceUrl());
        if (!alias.endsWith("/" + request.github().key().sourceId())
                || "0".equals(request.github().key().sourceNamespaceId())) {
            throw new IllegalArgumentException("The GitHub URL must match a positive repository ID and issue number");
        }
        return database.entities().executeAtomic(context -> {
            context.acquireAdvisoryTransactionLock(TaskBoardExternalReferenceAliasEntity.WRITE_LOCK);
            Set<UUID> candidates = new LinkedHashSet<>();
            Optional<TaskBoardExternalReferenceEntity> github = findByKey(context, request.github().key());
            github.map(row -> UUID.fromString(row.getTaskId())).ifPresent(candidates::add);
            var linearReference = request.linear().flatMap(value -> findByKey(context, value.key()));
            linearReference.map(row -> UUID.fromString(row.getTaskId())).ifPresent(candidates::add);
            Optional<TaskBoardExternalReferenceEntity> aliasReference = findByAlias(context, alias);
            aliasReference.map(row -> UUID.fromString(row.getTaskId())).ifPresent(candidates::add);
            context.findNamed(TaskBoardTaskEntity.class, TaskBoardTaskEntity.FIND_BY_GITHUB_ALIAS,
                    Map.of("alias", alias), 0).forEach(row -> candidates.add(row.toDomain().taskId()));
            request.existingTaskId().ifPresent(candidates::add);

            boolean created = candidates.isEmpty();
            UUID taskId = created ? UUID.randomUUID() : candidates.iterator().next();
            var githubObservation = request.github().forTask(taskId);
            var linearObservation = request.linear().map(value -> value.forTask(taskId));
            TaskBoardTask task;
            if (created) {
                if (githubObservation.expectedSyncRevision() != 0
                        || linearObservation.filter(value -> value.expectedSyncRevision() != 0).isPresent()) {
                    throw new TaskBoardConcurrentModificationException("A new source map requires revision zero");
                }
                Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
                task = new TaskBoardTask(taskId, request.projectKey(), request.sectionKey(), request.initialTitle(),
                        "", "", "", TaskBoardTaskStatus.BACKLOG, TaskBoardTaskPriority.NORMAL, TaskBoardQaStatus.NHV,
                        TaskBoardAudience.STAFF, 1, false, "", "", false,
                        actor.actorAccountId().orElseThrow(), actor.actorMinecraftUuid(), null, null, null, null, 0, now, now);
                context.save(TaskBoardTaskEntity.fromDomain(task, alias));
                context.save(TaskBoardAuditEntryEntity.fromDomain(taskAudit(actor, task, TaskBoardAuditEventType.TASK_CREATED)));
            } else {
                task = requireLockedTask(context, taskId);
                boolean aliasMismatch = aliasReference.map(TaskBoardExternalReferenceEntity::toDomain)
                        .filter(reference -> !reference.key().equals(request.github().key())).isPresent();
                boolean linearMismatch = linearObservation.filter(value -> hasDifferentProviderReference(context, value)).isPresent()
                        || linearReference.filter(row -> !row.getTaskId().equals(taskId.toString())).isPresent();
                boolean occupiedSlot = hasDifferentProviderReference(context, githubObservation) || linearMismatch;
                if (candidates.size() > 1 || aliasMismatch || occupiedSlot
                        || !task.projectKey().equals(request.projectKey())) {
                    Optional<UUID> other = candidates.stream().filter(id -> !id.equals(taskId)).findFirst();
                    var disputedObservation = linearMismatch ? linearObservation.orElseThrow() : githubObservation;
                    var result = conflict(context, disputedObservation,
                            taskAudit(actor, task, TaskBoardAuditEventType.EXTERNAL_REFERENCE_CONFLICTED),
                            Optional.empty(), other.or(() -> Optional.of(taskId)),
                            IDENTITY_CONFLICT + "the submitted source identities or QA scope disagree");
                    return new TaskBoardExternalReferenceIngestResult(Optional.empty(), false, List.of(result));
                }
            }

            var observations = new ArrayList<TaskBoardExternalReferenceMutationResult>();
            observations.add(reconcile(context, githubObservation,
                    taskAudit(actor, task, TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED), true));
            linearObservation.ifPresent(value -> observations.add(reconcile(context, value,
                    taskAudit(actor, task, TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED), true)));
            return new TaskBoardExternalReferenceIngestResult(Optional.of(taskId), created, observations);
        });
    }

    private TaskBoardAuditEntry taskAudit(TaskBoardAuditEntry actor, TaskBoardTask task, TaskBoardAuditEventType type) {
        return new TaskBoardAuditEntry(UUID.randomUUID(), Optional.of(task.taskId()), task.projectKey(),
                actor.actorAccountId(), actor.actorMinecraftUuid(), type, actor.summary(), clock.instant());
    }

    private static boolean hasDifferentProviderReference(
            IPostgresEntityOperationContext context, TaskBoardExternalReferenceMutationRequest request) {
        return findTaskProvider(context, request.taskId(), request.key().provider())
                .map(TaskBoardExternalReferenceEntity::toDomain)
                .filter(reference -> !reference.key().equals(request.key())).isPresent();
    }

    private static String requireGitHubAlias(String value) {
        return TaskBoardExternalReferenceAliasEntity.githubAlias(value)
                .orElseThrow(() -> new IllegalArgumentException("An exact GitHub issue URL or key is required"));
    }

    private static Optional<TaskBoardExternalReferenceEntity> findByAlias(
            IPostgresEntityOperationContext context, String alias) {
        return context.find(TaskBoardExternalReferenceAliasEntity.class, alias)
                .flatMap(row -> context.find(TaskBoardExternalReferenceEntity.class, row.referenceId()));
    }

    private static TaskBoardTask requireLockedTask(IPostgresEntityOperationContext context, UUID taskId) {
        return context.find(TaskBoardTaskEntity.class, taskId.toString(), LockModeType.PESSIMISTIC_READ)
                .orElseThrow(() -> new IllegalArgumentException("Task Board task not found: " + taskId)).toDomain();
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
            context.acquireAdvisoryTransactionLock(TaskBoardExternalReferenceAliasEntity.WRITE_LOCK);
            return reconcile(context, request, auditEntry, false);
        });
    }

    private TaskBoardExternalReferenceMutationResult reconcile(IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceMutationRequest request, TaskBoardAuditEntry auditEntry, boolean ingest) {
        TaskBoardTask task = requireLockedTask(context, request.taskId());
        if (!task.projectKey().equals(auditEntry.projectKey())) {
            throw new IllegalArgumentException("Audit project must match the referenced Task Board task");
        }

        Optional<TaskBoardExternalReferenceEntity> latest = findByKey(context, request.key());
        Optional<String> alias = request.key().provider() == TaskBoardExternalReferenceProvider.GITHUB_ISSUE
                ? TaskBoardExternalReferenceAliasEntity.githubAlias(request.sourceUrl()) : Optional.empty();
        Optional<TaskBoardExternalReferenceEntity> aliasReference = alias.flatMap(value -> findByAlias(context, value));
        if (aliasReference.map(TaskBoardExternalReferenceEntity::toDomain)
                .filter(reference -> !reference.key().equals(request.key())).isPresent()) {
            return conflict(context, request, auditEntry, Optional.empty(),
                    aliasReference.map(row -> UUID.fromString(row.getTaskId())), IDENTITY_CONFLICT + "GitHub alias is already linked");
        }
        if (latest.isPresent() && !latest.orElseThrow().getTaskId().equals(request.taskId().toString())) {
            return conflict(context, request, auditEntry, latest, Optional.of(UUID.fromString(latest.orElseThrow().getTaskId())),
                    "External identity is already linked to another QA task");
        }

        Optional<TaskBoardExternalReferenceEntity> current = latest;
        if (ingest) {
            current.map(TaskBoardExternalReferenceEntity::toDomain)
                    .ifPresent(reference -> rememberAlias(context, alias, reference));
        }
        if (current.isPresent() && isExactReplay(current.orElseThrow().toDomain(), request)) {
            rememberAlias(context, alias, current.orElseThrow().toDomain());
            return new TaskBoardExternalReferenceMutationResult(
                    Optional.of(current.orElseThrow().toDomain()), Optional.empty(), TaskBoardExternalReferenceMutationOutcome.REPLAYED);
        }

        if (current.map(TaskBoardExternalReferenceEntity::toDomain)
                .map(reference -> reference.syncRevision() != request.expectedSyncRevision())
                .orElse(request.expectedSyncRevision() != 0)) {
            return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                    "Expected sync revision does not match the current reference revision");
        }

        if (current.isEmpty()) {
            Optional<TaskBoardExternalReferenceEntity> occupiedProviderSlot =
                    findTaskProvider(context, request.taskId(), request.key().provider());
            if (occupiedProviderSlot.isPresent()
                    && !occupiedProviderSlot.orElseThrow().toDomain().key().equals(request.key())) {
                return conflict(context, request, auditEntry, Optional.empty(), Optional.of(request.taskId()),
                        "QA task already has a different external reference for this provider");
            }
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
        boolean providerDisagreement = otherProvider.map(TaskBoardExternalReferenceEntity::toDomain)
                .filter(other -> contradictory(other.sourceStatus(), request.sourceStatus())).isPresent();
        boolean contradictsExecution = request.sourceStatus().indicatesTerminalWork()
                && (task.status() == TaskBoardTaskStatus.ACTIVE || task.status() == TaskBoardTaskStatus.REVIEW);
        if (providerDisagreement || contradictsExecution) {
            if (ingest && current.isEmpty()) {
                // Reserve the stable identity even when its first observation requires review.
                Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
                var pending = new TaskBoardExternalReference(UUID.randomUUID(), request.taskId(), request.key(),
                        request.displayKey(), request.sourceUrl(), TaskBoardExternalReferenceSourceStatus.UNKNOWN,
                        request.sourceRevision(), "", request.sourceCursor(), "", TaskBoardExternalReferenceSyncState.PENDING,
                        1, request.observedAt(), request.observedAt(), now, now);
                current = Optional.of(context.save(TaskBoardExternalReferenceEntity.fromDomain(pending)));
                rememberAlias(context, alias, pending);
            }
            return conflict(context, request, auditEntry, current, Optional.of(request.taskId()),
                    providerDisagreement ? "Provider source statuses disagree between active work and terminal work"
                            : "External terminal status disagrees with active QA execution");
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
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
        rememberAlias(context, alias, saved);
        context.save(TaskBoardAuditEntryEntity.fromDomain(auditFor(auditEntry,
                TaskBoardAuditEventType.EXTERNAL_REFERENCE_UPSERTED,
                "Registered or reconciled " + request.key().provider() + " external reference " + request.key().stableKey())));
        return new TaskBoardExternalReferenceMutationResult(Optional.of(saved), Optional.empty(), outcome);
    }

    private static void rememberAlias(IPostgresEntityOperationContext context, Optional<String> alias,
            TaskBoardExternalReference reference) {
        alias.filter(value -> context.find(TaskBoardExternalReferenceAliasEntity.class, value).isEmpty())
                .ifPresent(value -> context.save(new TaskBoardExternalReferenceAliasEntity(
                        value, reference.referenceId().toString())));
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
            context.acquireAdvisoryTransactionLock(TaskBoardExternalReferenceAliasEntity.WRITE_LOCK);
            TaskBoardExternalReferenceConflictEntity conflictRow = context.find(
                            TaskBoardExternalReferenceConflictEntity.class, request.conflictId().toString(), LockModeType.PESSIMISTIC_WRITE)
                    .orElseThrow(() -> new IllegalArgumentException("External reference conflict not found: " + request.conflictId()));
            TaskBoardExternalReferenceConflict conflict = conflictRow.toDomain();
            if (conflict.status() == TaskBoardExternalReferenceConflictStatus.RESOLVED) {
                if (conflict.resolution().filter(request.resolution()::equals).isPresent()) return conflict;
                throw new TaskBoardConcurrentModificationException("External reference conflict was already resolved differently");
            }

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
            boolean applyIncoming = request.resolution() == TaskBoardExternalReferenceConflictResolution.APPLY_INCOMING;
            if (applyIncoming && conflict.reason().startsWith(IDENTITY_CONFLICT)) {
                throw new TaskBoardConcurrentModificationException("Resolve the source identity before resubmitting");
            }
            if (applyIncoming && current.isPresent() && !current.orElseThrow().taskId().equals(conflict.requestedTaskId())) {
                throw new TaskBoardConcurrentModificationException("External identity is now linked to another QA task");
            }

            Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
            boolean otherOpenConflict = hasOtherOpenConflict(context, conflict);
            if (applyIncoming) {
                Optional<TaskBoardExternalReferenceEntity> providerSlot = findTaskProvider(context, conflict.requestedTaskId(), conflict.key().provider());
                if (providerSlot.isPresent() && !providerSlot.orElseThrow().getSourceId().equals(conflict.key().sourceId())) {
                    throw new TaskBoardConcurrentModificationException("The QA task already has a different reference for this provider");
                }
                TaskBoardExternalReference applied = referenceFromConflict(conflict, current, now,
                        otherOpenConflict ? TaskBoardExternalReferenceSyncState.CONFLICT : TaskBoardExternalReferenceSyncState.CONNECTED);
                var alias = conflict.key().provider() == TaskBoardExternalReferenceProvider.GITHUB_ISSUE
                        ? TaskBoardExternalReferenceAliasEntity.githubAlias(conflict.incomingSourceUrl()) : Optional.<String>empty();
                if (alias.flatMap(value -> findByAlias(context, value)).map(TaskBoardExternalReferenceEntity::toDomain)
                        .filter(reference -> !reference.key().equals(conflict.key())).isPresent()) {
                    throw new TaskBoardConcurrentModificationException("GitHub alias is already linked");
                }
                context.save(TaskBoardExternalReferenceEntity.fromDomain(applied));
                rememberAlias(context, alias, applied);
            } else if (!conflict.reason().startsWith(IDENTITY_CONFLICT) && current.isPresent()
                    && current.orElseThrow().taskId().equals(conflict.requestedTaskId())) {
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
            IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceMutationRequest request,
            TaskBoardAuditEntry auditEntry,
            Optional<TaskBoardExternalReferenceEntity> current,
            Optional<UUID> existingTaskId,
            String reason
    ) {
        UUID conflictId = conflictId(request, existingTaskId, reason);
        Optional<TaskBoardExternalReferenceConflictEntity> alreadyQueued = context.find(
                TaskBoardExternalReferenceConflictEntity.class, conflictId.toString());
        if (alreadyQueued.isPresent()) {
            TaskBoardExternalReferenceConflict existingConflict = alreadyQueued.orElseThrow().toDomain();
            Optional<TaskBoardExternalReference> currentReference = reason.startsWith(IDENTITY_CONFLICT)
                    ? Optional.empty() : findByKey(context, request.key()).map(TaskBoardExternalReferenceEntity::toDomain);
            return new TaskBoardExternalReferenceMutationResult(currentReference,
                    Optional.of(existingConflict), TaskBoardExternalReferenceMutationOutcome.CONFLICT);
        }

        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
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
            IPostgresEntityOperationContext context,
            TaskBoardExternalReferenceKey key
    ) {
        return context.findOneNamed(TaskBoardExternalReferenceEntity.class,
                TaskBoardExternalReferenceEntity.FIND_BY_KEY, keyParameters(key));
    }

    private static Optional<TaskBoardExternalReferenceEntity> findTaskProvider(
            IPostgresEntityOperationContext context,
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

    private static UUID conflictId(TaskBoardExternalReferenceMutationRequest request, Optional<UUID> existingTaskId,
            String reason) {
        String fingerprint = request.key().stableKey() + "|" + request.taskId() + "|"
                + request.sourceRevision() + "|" + request.sourceFingerprint() + "|"
                + request.sourceStatus();
        if (reason.startsWith(IDENTITY_CONFLICT)) {
            fingerprint += "|identity|" + existingTaskId.map(UUID::toString).orElse("");
        }
        return UUID.nameUUIDFromBytes(fingerprint.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean hasOtherOpenConflict(
            IPostgresEntityOperationContext context,
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
