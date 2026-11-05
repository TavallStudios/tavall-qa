package org.tavall.qa.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class TaskBoardStorageUnavailableExceptionTest {
    @Test
    void retainsTheStorageFailureDescription() {
        TaskBoardStorageUnavailableException exception =
                new TaskBoardStorageUnavailableException("task board storage is unavailable");

        assertEquals("task board storage is unavailable", exception.getMessage());
    }
}
