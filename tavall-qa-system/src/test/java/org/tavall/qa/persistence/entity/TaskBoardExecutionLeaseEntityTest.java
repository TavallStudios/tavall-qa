package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardExecutionLeaseEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.lease(), TaskBoardExecutionLeaseEntity.fromDomain(TaskBoardEntitySamples.lease()).toDomain());
    }
}
