package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardExternalReferenceEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.externalReference(), TaskBoardExternalReferenceEntity.fromDomain(TaskBoardEntitySamples.externalReference()).toDomain());
    }
}
