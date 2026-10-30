package org.tavall.qa.handler;

import org.tavall.qa.domain.auth.QaSubjectContext;
import org.tavall.qa.domain.notion.NotionDatabaseTaskMapping;
import org.tavall.qa.domain.notion.NotionTaskIngestResult;
import org.tavall.qa.domain.notion.NotionTaskOutboundChange;
import org.tavall.qa.domain.notion.NotionTaskSnapshot;
import org.tavall.qa.domain.notion.NotionTaskSyncAckRequest;
import org.tavall.qa.domain.notion.NotionTaskSyncAckResult;
import org.tavall.qa.domain.notion.NotionTaskSyncState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owner API for Notion task synchronization.
 * Handles idempotent ingest, conflict detection/resolution, archived/deleted handling,
 * outbound pending-change export, and idempotent sync acknowledgement.
 */
public interface ITaskBoardNotionSyncHandler {
    NotionTaskIngestResult ingestSnapshot(QaSubjectContext subject, NotionTaskSnapshot snapshot);

    List<NotionTaskOutboundChange> findPendingOutboundChanges(QaSubjectContext subject, Optional<String> databaseId);

    NotionTaskSyncAckResult acknowledgeOutboundSync(QaSubjectContext subject, NotionTaskSyncAckRequest request);

    Optional<NotionTaskSyncState> findSyncStateByPageId(QaSubjectContext subject, String notionPageId);

    Optional<NotionTaskSyncState> findSyncStateByTaskId(QaSubjectContext subject, UUID taskId);

    void registerDatabaseMapping(QaSubjectContext subject, NotionDatabaseTaskMapping mapping);

    Optional<NotionDatabaseTaskMapping> findDatabaseMapping(String databaseId);
}
