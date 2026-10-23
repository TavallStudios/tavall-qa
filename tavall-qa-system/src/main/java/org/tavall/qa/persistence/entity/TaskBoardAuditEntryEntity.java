package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.TaskBoardAuditEventType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "task_board_audit")
@NamedQuery(
        name = TaskBoardAuditEntryEntity.FIND_BY_TASK,
        query = "SELECT entity FROM TaskBoardAuditEntryEntity entity WHERE entity.taskId = :taskId ORDER BY entity.occurredAt, entity.eventId"
)
public class TaskBoardAuditEntryEntity {
    public static final String FIND_BY_TASK = "TaskBoardAuditEntryEntity.findByTask";

    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "task_id", length = 36)
    private String taskId;

    @Column(name = "project_key", nullable = false, length = 96)
    private String projectKey;

    @Column(name = "actor_account_id", length = 36)
    private String actorAccountId;

    @Column(name = "actor_minecraft_uuid", length = 36)
    private String actorMinecraftUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private TaskBoardAuditEventType eventType;

    @Column(name = "summary", nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected TaskBoardAuditEntryEntity() {
    }

    private TaskBoardAuditEntryEntity(TaskBoardAuditEntry event) {
        eventId = event.eventId().toString();
        taskId = event.taskId().map(UUID::toString).orElse(null);
        projectKey = event.projectKey();
        actorAccountId = event.actorAccountId().map(UUID::toString).orElse(null);
        actorMinecraftUuid = uuidString(event.actorMinecraftUuid());
        eventType = event.eventType();
        summary = event.summary();
        occurredAt = event.occurredAt();
    }

    public static TaskBoardAuditEntryEntity fromDomain(TaskBoardAuditEntry event) {
        return new TaskBoardAuditEntryEntity(event);
    }

    public TaskBoardAuditEntry toDomain() {
        return new TaskBoardAuditEntry(
                UUID.fromString(eventId),
                Optional.ofNullable(uuid(taskId)),
                projectKey,
                Optional.ofNullable(uuid(actorAccountId)),
                uuid(actorMinecraftUuid),
                eventType,
                summary,
                occurredAt
        );
    }

    private static String uuidString(UUID value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
