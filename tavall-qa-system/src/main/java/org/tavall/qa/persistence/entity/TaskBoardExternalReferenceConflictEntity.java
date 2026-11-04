package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflict;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictResolution;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceConflictStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "task_board_external_reference_conflict")
@NamedQuery(
        name = TaskBoardExternalReferenceConflictEntity.FIND_BY_ID,
        query = "SELECT entity FROM TaskBoardExternalReferenceConflictEntity entity WHERE entity.conflictId = :conflictId"
)
@NamedQuery(
        name = TaskBoardExternalReferenceConflictEntity.FIND_OPEN,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceConflictEntity entity
                WHERE entity.status = :status
                ORDER BY entity.createdAt
                """
)
@NamedQuery(
        name = TaskBoardExternalReferenceConflictEntity.FIND_OPEN_FOR_TASK,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceConflictEntity entity
                WHERE entity.status = :status
                  AND (entity.requestedTaskId = :taskId OR entity.existingTaskId = :taskId)
                ORDER BY entity.createdAt
                """
)
@NamedQuery(
        name = TaskBoardExternalReferenceConflictEntity.FIND_OPEN_BY_REFERENCE,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceConflictEntity entity
                WHERE entity.referenceId = :referenceId
                  AND entity.status = :status
                ORDER BY entity.createdAt
                """
)
@NamedQuery(
        name = TaskBoardExternalReferenceConflictEntity.FIND_OPEN_BY_KEY,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceConflictEntity entity
                WHERE entity.provider = :provider
                  AND entity.sourceNamespaceId = :sourceNamespaceId
                  AND entity.sourceId = :sourceId
                  AND entity.status = :status
                ORDER BY entity.createdAt
                """
)
public class TaskBoardExternalReferenceConflictEntity {
    public static final String FIND_BY_ID = "TaskBoardExternalReferenceConflictEntity.findById";
    public static final String FIND_OPEN = "TaskBoardExternalReferenceConflictEntity.findOpen";
    public static final String FIND_OPEN_FOR_TASK = "TaskBoardExternalReferenceConflictEntity.findOpenForTask";
    public static final String FIND_OPEN_BY_REFERENCE = "TaskBoardExternalReferenceConflictEntity.findOpenByReference";
    public static final String FIND_OPEN_BY_KEY = "TaskBoardExternalReferenceConflictEntity.findOpenByKey";

    @Id
    @Column(name = "conflict_id", nullable = false, length = 36)
    private String conflictId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private TaskBoardExternalReferenceProvider provider;

    @Column(name = "source_namespace_id", nullable = false, length = 255)
    private String sourceNamespaceId;

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "existing_task_id", length = 36)
    private String existingTaskId;

    @Column(name = "requested_task_id", nullable = false, length = 36)
    private String requestedTaskId;

    @Column(name = "incoming_display_key", nullable = false, length = 255)
    private String incomingDisplayKey;

    @Column(name = "incoming_source_url", nullable = false, length = 2048)
    private String incomingSourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "incoming_source_status", nullable = false, length = 32)
    private TaskBoardExternalReferenceSourceStatus incomingSourceStatus;

    @Column(name = "incoming_source_revision", nullable = false, length = 255)
    private String incomingSourceRevision;

    @Column(name = "incoming_source_cursor", nullable = false, length = 512)
    private String incomingSourceCursor;

    @Column(name = "incoming_source_fingerprint", nullable = false, length = 128)
    private String incomingSourceFingerprint;

    @Column(name = "incoming_observed_at", nullable = false)
    private Instant incomingObservedAt;

    @Column(name = "expected_sync_revision", nullable = false)
    private long expectedSyncRevision;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TaskBoardExternalReferenceConflictStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 24)
    private TaskBoardExternalReferenceConflictResolution resolution;

    @Column(name = "resolved_by_account_id", length = 36)
    private String resolvedByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected TaskBoardExternalReferenceConflictEntity() {
    }

    public TaskBoardExternalReferenceConflict toDomain() {
        return new TaskBoardExternalReferenceConflict(
                UUID.fromString(conflictId),
                new TaskBoardExternalReferenceKey(provider, sourceNamespaceId, sourceId),
                optionalUuid(referenceId),
                optionalUuid(existingTaskId),
                UUID.fromString(requestedTaskId),
                incomingDisplayKey,
                incomingSourceUrl,
                incomingSourceStatus,
                incomingSourceRevision,
                incomingSourceCursor,
                incomingSourceFingerprint,
                incomingObservedAt,
                expectedSyncRevision,
                reason,
                status,
                Optional.ofNullable(resolution),
                optionalUuid(resolvedByAccountId),
                createdAt,
                Optional.ofNullable(resolvedAt)
        );
    }

    public static TaskBoardExternalReferenceConflictEntity fromDomain(TaskBoardExternalReferenceConflict conflict) {
        Objects.requireNonNull(conflict, "conflict");
        TaskBoardExternalReferenceConflictEntity entity = new TaskBoardExternalReferenceConflictEntity();
        entity.conflictId = conflict.conflictId().toString();
        entity.provider = conflict.key().provider();
        entity.sourceNamespaceId = conflict.key().sourceNamespaceId();
        entity.sourceId = conflict.key().sourceId();
        entity.referenceId = conflict.referenceId().map(UUID::toString).orElse(null);
        entity.existingTaskId = conflict.existingTaskId().map(UUID::toString).orElse(null);
        entity.requestedTaskId = conflict.requestedTaskId().toString();
        entity.incomingDisplayKey = conflict.incomingDisplayKey();
        entity.incomingSourceUrl = conflict.incomingSourceUrl();
        entity.incomingSourceStatus = conflict.incomingSourceStatus();
        entity.incomingSourceRevision = conflict.incomingSourceRevision();
        entity.incomingSourceCursor = conflict.incomingSourceCursor();
        entity.incomingSourceFingerprint = conflict.incomingSourceFingerprint();
        entity.incomingObservedAt = conflict.incomingObservedAt();
        entity.expectedSyncRevision = conflict.expectedSyncRevision();
        entity.reason = conflict.reason();
        entity.status = conflict.status();
        entity.resolution = conflict.resolution().orElse(null);
        entity.resolvedByAccountId = conflict.resolvedByAccountId().map(UUID::toString).orElse(null);
        entity.createdAt = conflict.createdAt();
        entity.resolvedAt = conflict.resolvedAt().orElse(null);
        return entity;
    }

    public String getConflictId() { return conflictId; }
    public String getRequestedTaskId() { return requestedTaskId; }
    public TaskBoardExternalReferenceConflictStatus getStatus() { return status; }

    private static Optional<UUID> optionalUuid(String value) {
        return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
    }
}
