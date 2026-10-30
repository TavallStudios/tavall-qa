package org.tavall.qa.domain.notion;

import org.tavall.qa.domain.TaskBoardTask;

import java.util.Objects;
import java.util.UUID;

/**
 * Result of ingesting a Notion task snapshot.
 */
public record NotionTaskIngestResult(
        UUID taskId,
        String notionPageId,
        boolean created,
        boolean updated,
        boolean duplicateIngest,
        boolean conflict,
        String message,
        TaskBoardTask task,
        NotionTaskSyncState syncState
) {
    public NotionTaskIngestResult {
        Objects.requireNonNull(taskId, "taskId");
        Objects.requireNonNull(notionPageId, "notionPageId");
        message = message == null ? "" : message.trim();
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(syncState, "syncState");
    }
}
