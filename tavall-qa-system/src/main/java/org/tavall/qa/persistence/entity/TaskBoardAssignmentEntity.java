package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.TaskBoardAssignment;
import org.tavall.qa.domain.auth.QaCapability;
import org.tavall.qa.domain.auth.QaClass;
import org.tavall.qa.domain.auth.QaEnvironmentGrant;
import org.tavall.qa.domain.auth.QaTier;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "task_board_assignment")
@NamedQuery(
        name = TaskBoardAssignmentEntity.FIND_BY_TASK,
        query = "SELECT entity FROM TaskBoardAssignmentEntity entity WHERE entity.taskId = :taskId ORDER BY entity.assignedAt"
)
@NamedQuery(
        name = TaskBoardAssignmentEntity.FIND_BY_ACCOUNT,
        query = "SELECT entity FROM TaskBoardAssignmentEntity entity WHERE entity.accountId = :accountId ORDER BY entity.assignedAt DESC"
)
public class TaskBoardAssignmentEntity {
    public static final String FIND_BY_TASK = "TaskBoardAssignmentEntity.findByTask";
    public static final String FIND_BY_ACCOUNT = "TaskBoardAssignmentEntity.findByAccount";

    @Id
    @Column(name = "assignment_id", nullable = false, length = 36)
    private String assignmentId;

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability", nullable = false, length = 48)
    private QaCapability capability;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 16)
    private QaEnvironmentGrant environment;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_qa_class", length = 32)
    private QaClass requiredClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "minimum_qa_tier", length = 16)
    private QaTier minimumTier;

    @Column(name = "assigned_by_account_id", nullable = false, length = 36)
    private String assignedByAccountId;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected TaskBoardAssignmentEntity() {
    }

    private TaskBoardAssignmentEntity(TaskBoardAssignment assignment) {
        assignmentId = assignment.assignmentId().toString();
        organizationId = assignment.organizationId().toString();
        taskId = assignment.taskId().toString();
        accountId = assignment.accountId().toString();
        capability = assignment.capability();
        environment = assignment.environment();
        requiredClass = assignment.requiredClass().orElse(null);
        minimumTier = assignment.minimumTier().orElse(null);
        assignedByAccountId = assignment.assignedByAccountId().toString();
        reason = assignment.reason();
        assignedAt = assignment.assignedAt();
        revokedAt = assignment.revokedAt();
    }

    public static TaskBoardAssignmentEntity fromDomain(TaskBoardAssignment assignment) {
        return new TaskBoardAssignmentEntity(assignment);
    }

    public TaskBoardAssignment toDomain() {
        return new TaskBoardAssignment(
                UUID.fromString(assignmentId),
                UUID.fromString(organizationId),
                UUID.fromString(taskId),
                UUID.fromString(accountId),
                capability,
                environment,
                Optional.ofNullable(requiredClass),
                Optional.ofNullable(minimumTier),
                UUID.fromString(assignedByAccountId),
                reason,
                assignedAt,
                revokedAt
        );
    }
}
