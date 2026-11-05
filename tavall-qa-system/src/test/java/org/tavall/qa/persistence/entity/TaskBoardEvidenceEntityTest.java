package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardEvidenceEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.evidence(), TaskBoardEvidenceEntity.fromDomain(TaskBoardEntitySamples.evidence()).toDomain());
    }
}
