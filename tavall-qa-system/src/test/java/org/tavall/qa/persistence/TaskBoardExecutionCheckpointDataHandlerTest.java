package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.handler.FakePostgresDatabaseBoundary;
import org.tavall.qa.handler.TaskBoardTestComposition;

import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardExecutionCheckpointDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void readsCurrentLeaseThroughTypedPostgresDependencyAccess() {
        try (var composition = TaskBoardTestComposition.openWithPostgresBoundary(
                tempDir, Clock.systemUTC(), FakePostgresDatabaseBoundary.available())) {
            ITaskBoardExecutionCheckpointDataHandler handler =
                    composition.bean(ITaskBoardExecutionCheckpointDataHandler.class);

            assertTrue(handler.currentLease(UUID.randomUUID()).isEmpty());
        }
    }
}
