package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.TaskBoardStorageUnavailableException;
import org.tavall.qa.handler.TaskBoardTestComposition;

import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UnavailableTaskBoardDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void configurationReturnsReadOnlyUnavailableCapabilityWithoutPostgres() {
        try (var composition = TaskBoardTestComposition.openWithoutTaskDatabase(tempDir, Clock.systemUTC())) {
            ITaskBoardDataHandler handler = composition.bean(ITaskBoardDataHandler.class);

            assertFalse(handler.isAvailable());
            assertTrue(handler.findProjects().isEmpty());
            assertThrows(TaskBoardStorageUnavailableException.class,
                    () -> handler.saveProject(null, null));
        }
    }
}
