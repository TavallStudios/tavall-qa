package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardAuditEntryEntityTest {
    @Test
    void domainMappingRoundTrips() {
        assertEquals(TaskBoardEntitySamples.auditEntry(), TaskBoardAuditEntryEntity.fromDomain(TaskBoardEntitySamples.auditEntry()).toDomain());
    }
}
