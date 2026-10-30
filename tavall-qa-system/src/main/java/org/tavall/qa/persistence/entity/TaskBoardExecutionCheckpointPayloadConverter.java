package org.tavall.qa.persistence.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;
import org.tavall.qa.mapping.TaskBoardExecutionCheckpointPayloadMapper;

/** JPA-owned pure value mapping; no DI lookup, I/O or application lifecycle is performed here. */
@Converter
public final class TaskBoardExecutionCheckpointPayloadConverter
        implements AttributeConverter<TaskBoardExecutionCheckpointPayloadData, String> {
    @Override
    public String convertToDatabaseColumn(TaskBoardExecutionCheckpointPayloadData value) {
        return TaskBoardExecutionCheckpointPayloadMapper.toJson(value);
    }

    @Override
    public TaskBoardExecutionCheckpointPayloadData convertToEntityAttribute(String value) {
        return TaskBoardExecutionCheckpointPayloadMapper.fromJson(value);
    }
}
