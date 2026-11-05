package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;
import org.tavall.qa.domain.execution.TaskBoardExecutionCheckpointPayloadData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardExecutionCheckpointPayloadEntityTest {
    @Test
    void verifiesImmutablePayloadDigestWhenLoading() {
        TaskBoardExecutionCheckpointPayloadData payload = TaskBoardEntitySamples.payload();
        TaskBoardExecutionCheckpointPayloadEntity entity = TaskBoardExecutionCheckpointPayloadEntity.fromDomain(
                TaskBoardEntitySamples.id(30), payload);

        assertEquals(payload, entity.toDomain(entity.payloadSha256()));
        assertThrows(IllegalStateException.class, () -> entity.toDomain("0".repeat(64)));
    }
}
