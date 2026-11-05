package org.tavall.qa.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardExecutionLeaseTokenEntityTest {
    @Test
    void createsIssuedTokenAndRejectsMissingLeaseOrIssueTime() {
        assertNotNull(TaskBoardExecutionLeaseTokenEntity.fromDomain(TaskBoardEntitySamples.lease(),
                TaskBoardEntitySamples.NOW));
        assertThrows(NullPointerException.class,
                () -> TaskBoardExecutionLeaseTokenEntity.fromDomain(null, TaskBoardEntitySamples.NOW));
        assertThrows(NullPointerException.class,
                () -> TaskBoardExecutionLeaseTokenEntity.fromDomain(TaskBoardEntitySamples.lease(), null));
    }
}
