package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.tavall.qa.domain.execution.TaskBoardExecutionLeaseData;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Immutable issued work-token identity fences delayed acquisition replays across lease replacement. */
@Entity
@Table(name = "task_board_execution_lease_token")
public class TaskBoardExecutionLeaseTokenEntity {
    @Id
    @Column(name = "lease_id", nullable = false, length = 36)
    private String leaseId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "owner_account_id", nullable = false, length = 36)
    private String ownerAccountId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "initial_expires_at", nullable = false)
    private Instant initialExpiresAt;

    protected TaskBoardExecutionLeaseTokenEntity() { }

    public static TaskBoardExecutionLeaseTokenEntity fromDomain(TaskBoardExecutionLeaseData lease, Instant issuedAt) {
        Objects.requireNonNull(lease, "lease");
        TaskBoardExecutionLeaseTokenEntity entity = new TaskBoardExecutionLeaseTokenEntity();
        entity.leaseId = lease.leaseId().toString();
        entity.taskId = lease.taskId().toString();
        entity.runId = lease.runId().toString();
        entity.ownerAccountId = lease.ownerAccountId().toString();
        entity.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt").truncatedTo(ChronoUnit.MICROS);
        entity.initialExpiresAt = lease.expiresAt().truncatedTo(ChronoUnit.MICROS);
        return entity;
    }
}
