package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.notion.NotionTaskSyncState;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notion_task_sync_state")
@NamedQuery(
        name = NotionTaskSyncStateEntity.FIND_BY_PAGE_ID,
        query = """
                SELECT entity
                FROM NotionTaskSyncStateEntity entity
                WHERE entity.notionPageId = :notionPageId
                """
)
@NamedQuery(
        name = NotionTaskSyncStateEntity.FIND_BY_TASK_ID,
        query = """
                SELECT entity
                FROM NotionTaskSyncStateEntity entity
                WHERE entity.taskId = :taskId
                """
)
@NamedQuery(
        name = NotionTaskSyncStateEntity.FIND_BY_DATABASE_ID,
        query = """
                SELECT entity
                FROM NotionTaskSyncStateEntity entity
                WHERE entity.notionDatabaseId = :notionDatabaseId
                ORDER BY entity.updatedAt DESC
                """
)
@NamedQuery(
        name = NotionTaskSyncStateEntity.FIND_PENDING_OUTBOUND,
        query = """
                SELECT entity
                FROM NotionTaskSyncStateEntity entity
                WHERE entity.lastOutboundRevision > entity.lastAcknowledgedOutboundRevision
                ORDER BY entity.lastOutboundRevision ASC
                """
)
public class NotionTaskSyncStateEntity {
    public static final String FIND_BY_PAGE_ID = "NotionTaskSyncStateEntity.findByPageId";
    public static final String FIND_BY_TASK_ID = "NotionTaskSyncStateEntity.findByTaskId";
    public static final String FIND_BY_DATABASE_ID = "NotionTaskSyncStateEntity.findByDatabaseId";
    public static final String FIND_PENDING_OUTBOUND = "NotionTaskSyncStateEntity.findPendingOutbound";

    @Id
    @Column(name = "sync_id", nullable = false, length = 36)
    private String syncId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "notion_database_id", nullable = false, length = 64)
    private String notionDatabaseId;

    @Column(name = "notion_page_id", nullable = false, length = 64, unique = true)
    private String notionPageId;

    @Column(name = "notion_page_url", nullable = false, length = 2048)
    private String notionPageUrl;

    @Column(name = "last_ingested_revision")
    private Instant lastIngestedRevision;

    @Column(name = "last_outbound_revision", nullable = false)
    private long lastOutboundRevision;

    @Column(name = "last_acknowledged_outbound_revision", nullable = false)
    private long lastAcknowledgedOutboundRevision;

    @Column(name = "has_conflict", nullable = false)
    private boolean hasConflict;

    @Column(name = "conflict_details", nullable = false, columnDefinition = "text")
    private String conflictDetails;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotionTaskSyncStateEntity() {
    }

    public NotionTaskSyncState toDomain() {
        return new NotionTaskSyncState(
                UUID.fromString(syncId),
                UUID.fromString(taskId),
                notionDatabaseId,
                notionPageId,
                notionPageUrl,
                lastIngestedRevision,
                lastOutboundRevision,
                lastAcknowledgedOutboundRevision,
                hasConflict,
                conflictDetails,
                archived,
                deleted,
                createdAt,
                updatedAt
        );
    }

    public static NotionTaskSyncStateEntity fromDomain(NotionTaskSyncState domain) {
        Objects.requireNonNull(domain, "domain");
        NotionTaskSyncStateEntity entity = new NotionTaskSyncStateEntity();
        entity.syncId = domain.syncId().toString();
        entity.taskId = domain.taskId().toString();
        entity.notionDatabaseId = domain.notionDatabaseId();
        entity.notionPageId = domain.notionPageId();
        entity.notionPageUrl = domain.notionPageUrl();
        entity.lastIngestedRevision = domain.lastIngestedRevision();
        entity.lastOutboundRevision = domain.lastOutboundRevision();
        entity.lastAcknowledgedOutboundRevision = domain.lastAcknowledgedOutboundRevision();
        entity.hasConflict = domain.hasConflict();
        entity.conflictDetails = domain.conflictDetails();
        entity.archived = domain.archived();
        entity.deleted = domain.deleted();
        entity.createdAt = domain.createdAt();
        entity.updatedAt = domain.updatedAt();
        return entity;
    }

    public String getSyncId() {
        return syncId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getNotionDatabaseId() {
        return notionDatabaseId;
    }

    public String getNotionPageId() {
        return notionPageId;
    }

    public String getNotionPageUrl() {
        return notionPageUrl;
    }

    public Instant getLastIngestedRevision() {
        return lastIngestedRevision;
    }

    public long getLastOutboundRevision() {
        return lastOutboundRevision;
    }

    public long getLastAcknowledgedOutboundRevision() {
        return lastAcknowledgedOutboundRevision;
    }

    public boolean isHasConflict() {
        return hasConflict;
    }

    public String getConflictDetails() {
        return conflictDetails;
    }

    public boolean isArchived() {
        return archived;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
