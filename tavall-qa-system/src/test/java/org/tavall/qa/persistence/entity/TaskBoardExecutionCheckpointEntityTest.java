package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardExecutionCheckpointEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.checkpoint(), TaskBoardExecutionCheckpointEntity.fromDomain(TaskBoardEntitySamples.checkpoint()).toDomain());
    }
}
