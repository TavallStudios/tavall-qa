package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardTaskEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.task(), TaskBoardTaskEntity.fromDomain(TaskBoardEntitySamples.task(), "external/90").toDomain());
        assertEquals("external/90", TaskBoardTaskEntity.fromDomain(TaskBoardEntitySamples.task(), "external/90").externalKey());
    }
}
