package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardAssignmentEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.assignment(), TaskBoardAssignmentEntity.fromDomain(TaskBoardEntitySamples.assignment()).toDomain());
    }
}
