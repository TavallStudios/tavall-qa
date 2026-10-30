package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.tavall.qa.domain.execution.TaskBoardExecutionSourceData;
import org.tavall.qa.domain.execution.TaskBoardExecutionStatus;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointData;

import java.time.Instant;
import java.util.UUID;

/** Durable representation; transactions and entity lifecycle remain owned by Tavall Database. */
@Entity
@Table(name = "task_board_execution_checkpoint")
@NamedQuery(name = TaskBoardExecutionCheckpointEntity.FIND_BY_RUN,
        query = "SELECT entity FROM TaskBoardExecutionCheckpointEntity entity "
                + "WHERE entity.taskId = :taskId AND entity.runId = :runId ORDER BY entity.sequence DESC")
public class TaskBoardExecutionCheckpointEntity {
    public static final String FIND_BY_RUN = "TaskBoardExecutionCheckpointEntity.findByRun";

    @Id
    @Column(name = "checkpoint_id", nullable = false, length = 36)
    private String checkpointId;

    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;

    @Column(name = "run_id", nullable = false, length = 36)
    private String runId;

    @Column(name = "lease_id", nullable = false, length = 36)
    private String leaseId;

    @Column(name = "checkpoint_sequence", nullable = false)
    private long sequence;

    @Column(name = "task_fingerprint", nullable = false, length = 64)
    private String taskFingerprint;

    @Column(name = "source_repository", nullable = false, length = 201)
    private String sourceRepository;

    @Column(name = "source_branch", nullable = false, length = 256)
    private String sourceBranch;

    @Column(name = "source_commit", nullable = false, length = 40)
    private String sourceCommit;

    @Column(name = "diff_sha256", nullable = false, length = 64)
    private String diffSha256;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Column(name = "provider_id", nullable = false, length = 96)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 32)
    private TaskBoardExecutionStatus status;

    @Column(name = "recorded_by_account_id", nullable = false, length = 36)
    private String recordedByAccountId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected TaskBoardExecutionCheckpointEntity() {
    }

    public static TaskBoardExecutionCheckpointEntity fromDomain(TaskBoardExecutionCheckpointData data) {
        TaskBoardExecutionCheckpointEntity entity = new TaskBoardExecutionCheckpointEntity();
        entity.checkpointId = data.checkpointId().toString();
        entity.taskId = data.taskId().toString();
        entity.runId = data.runId().toString();
        entity.leaseId = data.leaseId().toString();
        entity.sequence = data.sequence();
        entity.taskFingerprint = data.taskFingerprint();
        entity.sourceRepository = data.source().repository();
        entity.sourceBranch = data.source().branch();
        entity.sourceCommit = data.source().commit();
        entity.diffSha256 = data.source().diffSha256();
        entity.payloadSha256 = data.payloadSha256();
        entity.providerId = data.providerId();
        entity.status = data.status();
        entity.recordedByAccountId = data.recordedByAccountId().toString();
        entity.recordedAt = data.recordedAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        return entity;
    }

    public TaskBoardExecutionCheckpointData toDomain() {
        return new TaskBoardExecutionCheckpointData(
                UUID.fromString(checkpointId),
                UUID.fromString(taskId),
                UUID.fromString(runId),
                UUID.fromString(leaseId),
                sequence,
                taskFingerprint,
                new TaskBoardExecutionSourceData(sourceRepository, sourceBranch, sourceCommit, diffSha256),
                payloadSha256,
                providerId,
                status,
                UUID.fromString(recordedByAccountId),
                recordedAt);
    }
}
