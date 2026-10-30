package org.tavall.qa.persistence;

import org.tavall.qa.domain.TaskBoardAuditEntry;
import org.tavall.qa.domain.notion.NotionTaskSyncState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence contract for Notion task synchronization state.
 */
public interface ITaskBoardNotionSyncDataHandler {
    boolean isAvailable();

    Optional<NotionTaskSyncState> findSyncStateByPageId(String pageId);

    Optional<NotionTaskSyncState> findSyncStateByTaskId(UUID taskId);

    List<NotionTaskSyncState> findSyncStatesByDatabaseId(String databaseId);

    List<NotionTaskSyncState> findPendingOutboundSyncStates(Optional<String> databaseId);

    NotionTaskSyncState saveSyncState(NotionTaskSyncState syncState, TaskBoardAuditEntry auditEntry);
}
