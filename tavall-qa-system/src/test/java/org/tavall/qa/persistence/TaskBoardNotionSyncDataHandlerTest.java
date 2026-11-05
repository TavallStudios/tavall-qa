package org.tavall.qa.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tavall.qa.domain.notion.NotionTaskSyncState;
import org.tavall.qa.handler.TaskBoardTestComposition;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TaskBoardNotionSyncDataHandlerTest {
    @TempDir Path tempDir;

    @Test
    void persistsAndRecoversSyncStateThroughTheConfiguredFileBoundary() {
        Instant now = Instant.parse("2026-11-01T12:00:00Z");
        NotionTaskSyncState expected = new NotionTaskSyncState(UUID.randomUUID(), UUID.randomUUID(),
                "qa-database", "combat-page", "https://notion.example/combat-page", now,
                2, 1, false, "", false, false, now, now);

        try (var composition = TaskBoardTestComposition.open(tempDir, Clock.fixed(now, java.time.ZoneOffset.UTC))) {
            ITaskBoardNotionSyncDataHandler handler = composition.bean(ITaskBoardNotionSyncDataHandler.class);
            assertTrue(handler.isAvailable());
            handler.saveSyncState(expected, null);
        }
        try (var restarted = TaskBoardTestComposition.open(tempDir, Clock.fixed(now, java.time.ZoneOffset.UTC))) {
            ITaskBoardNotionSyncDataHandler handler = restarted.bean(ITaskBoardNotionSyncDataHandler.class);
            assertEquals(expected, handler.findSyncStateByPageId(expected.notionPageId()).orElseThrow());
        }
    }
}
