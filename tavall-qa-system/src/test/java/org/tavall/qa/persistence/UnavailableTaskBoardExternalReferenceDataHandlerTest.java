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

final class UnavailableTaskBoardExternalReferenceDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void configurationReturnsReadOnlyUnavailableReferenceCapabilityWithoutPostgres() {
        try (var composition = TaskBoardTestComposition.openWithoutTaskDatabase(tempDir, Clock.systemUTC())) {
            ITaskBoardExternalReferenceDataHandler handler =
                    composition.bean(ITaskBoardExternalReferenceDataHandler.class);

            assertFalse(handler.isAvailable());
            assertTrue(handler.findOpenConflicts(java.util.Optional.empty()).isEmpty());
            assertThrows(TaskBoardStorageUnavailableException.class,
                    () -> handler.registerOrReconcile(null, null));
        }
    }
}
