package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardExecutionCheckpointPayloadConverterTest {
    @Test
    void convertsTypedPayloadToJsonAndBack() {
        TaskBoardExecutionCheckpointPayloadData payload = TaskBoardEntitySamples.payload();
        TaskBoardExecutionCheckpointPayloadConverter converter = new TaskBoardExecutionCheckpointPayloadConverter();

        String databaseValue = converter.convertToDatabaseColumn(payload);

        assertEquals(payload, converter.convertToEntityAttribute(databaseValue));
    }
}
