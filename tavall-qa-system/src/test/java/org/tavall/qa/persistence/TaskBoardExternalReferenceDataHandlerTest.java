package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceKey;
import org.tavall.qa.domain.external.TaskBoardExternalReferenceProvider;
import org.tavall.qa.handler.FakePostgresDatabaseBoundary;
import org.tavall.qa.handler.TaskBoardTestComposition;

import java.nio.file.Path;
import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardExternalReferenceDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void looksUpExternalReferencesThroughTheConfiguredPostgresBoundary() {
        try (var composition = TaskBoardTestComposition.openWithPostgresBoundary(
                tempDir, Clock.systemUTC(), FakePostgresDatabaseBoundary.available())) {
            ITaskBoardExternalReferenceDataHandler handler =
                    composition.bean(ITaskBoardExternalReferenceDataHandler.class);
            var key = new TaskBoardExternalReferenceKey(TaskBoardExternalReferenceProvider.GITHUB_ISSUE, "12345", "67890");

            assertTrue(handler.isAvailable());
            assertTrue(handler.findReference(key).isEmpty());
        }
    }
}
