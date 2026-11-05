package org.tavall.qa.handler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.auth.QaSubjectContext;

import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class TaskBoardExecutionCheckpointHandlerTest {
    @TempDir Path tempDir;
    private TaskBoardTestComposition composition;

    @BeforeEach
    void setUp() {
        composition = TaskBoardTestComposition.open(tempDir, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        composition.close();
    }

    @Test
    void anonymousSubjectCannotReadCurrentLease() {
        ITaskBoardExecutionCheckpointHandler handler = composition.bean(ITaskBoardExecutionCheckpointHandler.class);

        assertThrows(SecurityException.class,
                () -> handler.currentLease(QaSubjectContext.anonymous(), UUID.randomUUID()));
    }
}
