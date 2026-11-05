package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardVqcContributionEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.contribution(), TaskBoardVqcContributionEntity.fromDomain(TaskBoardEntitySamples.contribution()).toDomain());
    }
}
