package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.external.TaskBoardExternalReference;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSourceStatus;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceSyncState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "task_board_external_reference")
@NamedQuery(
        name = TaskBoardExternalReferenceEntity.FIND_BY_KEY,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceEntity entity
                WHERE entity.provider = :provider
                  AND entity.sourceNamespaceId = :sourceNamespaceId
                  AND entity.sourceId = :sourceId
                """
)
@NamedQuery(
        name = TaskBoardExternalReferenceEntity.FIND_BY_TASK,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceEntity entity
                WHERE entity.taskId = :taskId
                ORDER BY entity.provider
                """
)
@NamedQuery(
        name = TaskBoardExternalReferenceEntity.FIND_BY_TASK_PROVIDER,
        query = """
                SELECT entity
                FROM TaskBoardExternalReferenceEntity entity
                WHERE entity.taskId = :taskId
                  AND entity.provider = :provider
                """
)
public class TaskBoardExternalReferenceEntity {
    public static final String FIND_BY_KEY = "TaskBoardExternalReferenceEntity.findByKey";
    public static final String FIND_BY_TASK = "TaskBoardExternalReferenceEntity.findByTask";
    public static final String FIND_BY_TASK_PROVIDER = "TaskBoardExternalReferenceEntity.findByTaskProvider";

    @Id
    @Column(name = "reference_id", nullable = false, length = 36)
    private String referenceId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private TaskBoardExternalReferenceProvider provider;

    @Column(name = "source_namespace_id", nullable = false, length = 255)
    private String sourceNamespaceId;

    @Column(name = "source_id", nullable = false, length = 255)
    private String sourceId;

    @Column(name = "display_key", nullable = false, length = 255)
    private String displayKey;

    @Column(name = "source_url", nullable = false, length = 2048)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_status", nullable = false, length = 32)
    private TaskBoardExternalReferenceSourceStatus sourceStatus;

    @Column(name = "last_observed_revision", nullable = false, length = 255)
    private String lastObservedRevision;

    @Column(name = "last_applied_revision", nullable = false, length = 255)
    private String lastAppliedRevision;

    @Column(name = "last_observed_cursor", nullable = false, length = 512)
    private String lastObservedCursor;

    @Column(name = "source_fingerprint", nullable = false, length = 128)
    private String sourceFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_state", nullable = false, length = 32)
    private TaskBoardExternalReferenceSyncState syncState;

    @Column(name = "sync_revision", nullable = false)
    private long syncRevision;

    @Column(name = "last_observed_at", nullable = false)
    private Instant lastObservedAt;

    @Column(name = "last_applied_at", nullable = false)
    private Instant lastAppliedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskBoardExternalReferenceEntity() {
    }

    public TaskBoardExternalReference toDomain() {
        return new TaskBoardExternalReference(
                UUID.fromString(referenceId),
                UUID.fromString(taskId),
                new TaskBoardExternalReferenceKey(provider, sourceNamespaceId, sourceId),
                displayKey,
                sourceUrl,
                sourceStatus,
                lastObservedRevision,
                lastAppliedRevision,
                lastObservedCursor,
                sourceFingerprint,
                syncState,
                syncRevision,
                lastObservedAt,
                lastAppliedAt,
                createdAt,
                updatedAt
        );
    }

    public static TaskBoardExternalReferenceEntity fromDomain(TaskBoardExternalReference reference) {
        Objects.requireNonNull(reference, "reference");
        TaskBoardExternalReferenceEntity entity = new TaskBoardExternalReferenceEntity();
        entity.referenceId = reference.referenceId().toString();
        entity.taskId = reference.taskId().toString();
        entity.provider = reference.key().provider();
        entity.sourceNamespaceId = reference.key().sourceNamespaceId();
        entity.sourceId = reference.key().sourceId();
        entity.displayKey = reference.displayKey();
        entity.sourceUrl = reference.sourceUrl();
        entity.sourceStatus = reference.sourceStatus();
        entity.lastObservedRevision = reference.lastObservedRevision();
        entity.lastAppliedRevision = reference.lastAppliedRevision();
        entity.lastObservedCursor = reference.lastObservedCursor();
        entity.sourceFingerprint = reference.sourceFingerprint();
        entity.syncState = reference.syncState();
        entity.syncRevision = reference.syncRevision();
        entity.lastObservedAt = reference.lastObservedAt();
        entity.lastAppliedAt = reference.lastAppliedAt();
        entity.createdAt = reference.createdAt();
        entity.updatedAt = reference.updatedAt();
        return entity;
    }

    public String getReferenceId() { return referenceId; }
    public String getTaskId() { return taskId; }
    public TaskBoardExternalReferenceProvider getProvider() { return provider; }
    public String getSourceNamespaceId() { return sourceNamespaceId; }
    public String getSourceId() { return sourceId; }
}
