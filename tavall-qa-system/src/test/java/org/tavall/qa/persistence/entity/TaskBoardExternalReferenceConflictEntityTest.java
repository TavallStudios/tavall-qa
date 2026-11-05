package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardExternalReferenceConflictEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.externalConflict(), TaskBoardExternalReferenceConflictEntity.fromDomain(TaskBoardEntitySamples.externalConflict()).toDomain());
    }
}
