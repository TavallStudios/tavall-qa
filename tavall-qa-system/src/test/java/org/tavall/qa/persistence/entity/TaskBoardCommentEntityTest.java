package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardCommentEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.comment(), TaskBoardCommentEntity.fromDomain(TaskBoardEntitySamples.comment()).toDomain());
    }
}
