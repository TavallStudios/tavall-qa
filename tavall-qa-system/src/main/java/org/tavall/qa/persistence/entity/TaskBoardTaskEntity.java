package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardAudience;
import org.tavall.qa.domain.TaskBoardQaStatus;
import org.tavall.qa.domain.TaskBoardTask;
import org.tavall.qa.domain.TaskBoardTaskPriority;
import org.tavall.qa.domain.TaskBoardTaskStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_board_task")
@NamedQuery(
        name = TaskBoardTaskEntity.FIND_BY_PROJECT,
        query = """
                SELECT entity
                FROM TaskBoardTaskEntity entity
                WHERE entity.projectKey = :projectKey
                ORDER BY entity.sortOrder, entity.updatedAt DESC
                """
)
public class TaskBoardTaskEntity {
    public static final String FIND_BY_PROJECT =
            "TaskBoardTaskEntity.findByProject";

    /** Shares the canonical task mutation lock with durable execution state transitions. */
    public static String lockKey(UUID taskId) {
        return "tavall-qa:task:" + java.util.Objects.requireNonNull(taskId, "taskId");
    }

    @Id
    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "project_key", nullable = false, length = 96)
    private String projectKey;

    @Column(name = "section_key", nullable = false, length = 128)
    private String sectionKey;

    @Column(name = "title", nullable = false, length = 220)
    private String title;

    @Column(name = "public_title", nullable = false, length = 220)
    private String publicTitle;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "public_summary", nullable = false, columnDefinition = "text")
    private String publicSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false, length = 32)
    private TaskBoardTaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 32)
    private TaskBoardTaskPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "qa_status", nullable = false, length = 32)
    private TaskBoardQaStatus qaStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience", nullable = false, length = 32)
    private TaskBoardAudience audience;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "host_validation_required", nullable = false)
    private boolean hostValidationRequired;

    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;

    @Column(name = "source_reference", nullable = false, length = 1024)
    private String sourceReference;

    @Column(name = "qa_qc_eligible", nullable = false)
    private boolean qaQcEligible;

    @Column(name = "qa_revision", nullable = false)
    private int qaRevision;

    @Column(name = "created_by_account_id", length = 36)
    private String createdByAccountId;

    @Column(name = "created_by_minecraft_uuid", length = 36)
    private String createdByMinecraftUUID;

    @Column(name = "verified_by_account_id", length = 36)
    private String verifiedByAccountId;

    @Column(name = "verified_by_minecraft_uuid", length = 36)
    private String verifiedByMinecraftUUID;

    @Column(name = "verified_evidence_id", length = 36)
    private String verifiedEvidenceId;

    @Column(name = "human_verified_at")
    private Instant humanVerifiedAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TaskBoardTaskEntity() {
    }

    private TaskBoardTaskEntity(TaskBoardTask task) {
        taskId = task.taskId().toString();
        projectKey = task.projectKey();
        sectionKey = task.sectionKey();
        title = task.title();
        publicTitle = task.publicTitle();
        description = task.description();
        publicSummary = task.publicSummary();
        status = task.status();
        priority = task.priority();
        qaStatus = task.qaStatus();
        audience = task.audience();
        weight = task.weight();
        hostValidationRequired = task.hostValidationRequired();
        sourceType = task.sourceType();
        sourceReference = task.sourceReference();
        qaQcEligible = task.qaQcEligible();
        qaRevision = task.qaRevision();
        createdByAccountId = uuidString(task.createdByAccountId());
        createdByMinecraftUUID = uuidString(task.createdByMinecraftUUID());
        verifiedByAccountId = uuidString(task.verifiedByAccountId());
        verifiedByMinecraftUUID = uuidString(task.verifiedByMinecraftUUID());
        verifiedEvidenceId = uuidString(task.verifiedEvidenceId());
        humanVerifiedAt = task.humanVerifiedAt();
        sortOrder = task.sortOrder();
        createdAt = task.createdAt();
        updatedAt = task.updatedAt();
    }

    public static TaskBoardTaskEntity fromDomain(TaskBoardTask task) {
        return new TaskBoardTaskEntity(task);
    }

    public TaskBoardTask toDomain() {
        return new TaskBoardTask(
                UUID.fromString(taskId),
                projectKey,
                sectionKey,
                title,
                publicTitle,
                description,
                publicSummary,
                status,
                priority,
                qaStatus,
                audience,
                weight,
                hostValidationRequired,
                sourceType,
                sourceReference,
                qaQcEligible,
                qaRevision,
                uuid(createdByAccountId),
                uuid(createdByMinecraftUUID),
                uuid(verifiedByAccountId),
                uuid(verifiedByMinecraftUUID),
                uuid(verifiedEvidenceId),
                humanVerifiedAt,
                sortOrder,
                createdAt,
                updatedAt
        );
    }

    private static String uuidString(UUID value) {
        return value == null ? null : value.toString();
    }

    private static UUID uuid(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}
