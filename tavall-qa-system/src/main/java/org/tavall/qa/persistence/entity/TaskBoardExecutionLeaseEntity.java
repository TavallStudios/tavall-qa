package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;

import java.time.Instant;
import java.util.UUID;

/** Durable representation; transactions and entity lifecycle remain owned by Tavall Database. */
@Entity
@Table(name = "task_board_execution_lease")
@NamedQuery(
        name = TaskBoardExecutionLeaseEntity.FIND_ALL,
        query = "SELECT entity FROM TaskBoardExecutionLeaseEntity entity"
)
public class TaskBoardExecutionLeaseEntity {
    public static final String FIND_ALL = "TaskBoardExecutionLeaseEntity.findAll";
    @Id
    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "lease_id", nullable = false, length = 36)
    private String leaseId;

    @Column(name = "owner_account_id", nullable = false, length = 36)
    private String ownerAccountId;

    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "checkpoint_sequence", nullable = false)
    private long checkpointSequence;

    @Column(name = "task_fingerprint", nullable = false, length = 64)
    private String taskFingerprint;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected TaskBoardExecutionLeaseEntity() {
    }

    public static TaskBoardExecutionLeaseEntity fromDomain(TaskBoardExecutionLeaseData data) {
        TaskBoardExecutionLeaseEntity entity = new TaskBoardExecutionLeaseEntity();
        entity.taskId = data.taskId().toString();
        entity.runId = data.runId().toString();
        entity.leaseId = data.leaseId().toString();
        entity.ownerAccountId = data.ownerAccountId().toString();
        entity.revision = data.revision();
        entity.checkpointSequence = data.checkpointSequence();
        entity.taskFingerprint = data.taskFingerprint();
        entity.expiresAt = data.expiresAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return entity;
    }

    public TaskBoardExecutionLeaseData toDomain() {
        return new TaskBoardExecutionLeaseData(
                UUID.fromString(taskId),
                UUID.fromString(runId),
                UUID.fromString(leaseId),
                UUID.fromString(ownerAccountId),
                revision,
                checkpointSequence,
                taskFingerprint,
                expiresAt);
    }
}
