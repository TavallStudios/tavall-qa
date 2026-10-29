package org.tavall.qa.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.mapping.TaskBoardExecutionCheckpointPayloadMapper;

import java.util.Objects;
import java.util.UUID;

/** Immutable bulk context is separate from checkpoint history so metadata queries do not load payloads. */
@Entity
@Table(name = "task_board_execution_checkpoint_payload")
public class TaskBoardExecutionCheckpointPayloadEntity {
    @Id
    @Column(name = "checkpoint_id", nullable = false, length = 36)
    private String checkpointId;

    @Column(name = "payload_sha256", nullable = false, length = 64)
    private String payloadSha256;

    @Convert(converter = TaskBoardExecutionCheckpointPayloadConverter.class)
    @Column(name = "payload_json", nullable = false, columnDefinition = "text")
    private TaskBoardExecutionCheckpointPayloadData payload;

    protected TaskBoardExecutionCheckpointPayloadEntity() { }

    public static TaskBoardExecutionCheckpointPayloadEntity fromDomain(UUID checkpointId,
            TaskBoardExecutionCheckpointPayloadData payload) {
        TaskBoardExecutionCheckpointPayloadEntity entity = new TaskBoardExecutionCheckpointPayloadEntity();
        entity.checkpointId = Objects.requireNonNull(checkpointId, "checkpointId").toString();
        entity.payload = Objects.requireNonNull(payload, "payload");
        entity.payloadSha256 = TaskBoardExecutionCheckpointPayloadMapper.sha256(payload);
        return entity;
    }

    public String payloadSha256() { return payloadSha256; }

    public TaskBoardExecutionCheckpointPayloadData toDomain(String expectedSha256) {
        if (!Objects.requireNonNull(expectedSha256, "expectedSha256").equals(payloadSha256)
                || !payloadSha256.equals(TaskBoardExecutionCheckpointPayloadMapper.sha256(payload))) {
            throw new IllegalStateException("Checkpoint payload differs from its recorded immutable digest");
        }
        return payload;
    }

}
