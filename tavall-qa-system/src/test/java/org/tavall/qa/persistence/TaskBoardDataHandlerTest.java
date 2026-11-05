package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.handler.FakePostgresDatabaseBoundary;
import org.tavall.qa.handler.TaskBoardTestComposition;

import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class TaskBoardDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void readsProjectsThroughTheConfiguredPostgresBoundary() {
        try (var composition = TaskBoardTestComposition.openWithPostgresBoundary(
                tempDir, Clock.systemUTC(), FakePostgresDatabaseBoundary.available())) {
            ITaskBoardDataHandler handler = composition.bean(ITaskBoardDataHandler.class);

            assertTrue(handler.isAvailable());
            assertEquals(java.util.List.of(), handler.findProjects());
        }
    }
}
