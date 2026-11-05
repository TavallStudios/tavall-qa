package org.tavall.qa.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class TaskBoardConcurrentModificationExceptionTest {
    @Test
    void retainsTheConcurrentModificationDescription() {
        TaskBoardConcurrentModificationException exception =
                new TaskBoardConcurrentModificationException("task changed before update");

        assertEquals("task changed before update", exception.getMessage());
    }
}
