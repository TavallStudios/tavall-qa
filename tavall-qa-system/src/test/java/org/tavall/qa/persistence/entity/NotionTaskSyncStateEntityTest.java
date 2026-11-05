package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class NotionTaskSyncStateEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.notionState(), NotionTaskSyncStateEntity.fromDomain(TaskBoardEntitySamples.notionState()).toDomain());
    }
}
