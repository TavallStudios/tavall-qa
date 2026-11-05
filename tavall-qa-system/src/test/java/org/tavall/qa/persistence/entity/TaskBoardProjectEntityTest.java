package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardProjectEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.project(), TaskBoardProjectEntity.fromDomain(TaskBoardEntitySamples.project()).toDomain());
    }
}
