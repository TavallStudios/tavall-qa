CREATE TABLE IF NOT EXISTS notion_task_sync_state (
    sync_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    notion_database_id VARCHAR(64) NOT NULL,
    notion_page_id VARCHAR(64) NOT NULL,
    notion_page_url VARCHAR(2048) NOT NULL,
    last_ingested_revision TIMESTAMPTZ,
    last_outbound_revision BIGINT NOT NULL DEFAULT 0,
    last_acknowledged_outbound_revision BIGINT NOT NULL DEFAULT 0,
    has_conflict BOOLEAN NOT NULL DEFAULT FALSE,
    conflict_details TEXT NOT NULL DEFAULT '',
    is_archived BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_notion_sync_page_id UNIQUE (notion_page_id)
);

CREATE INDEX IF NOT EXISTS idx_notion_sync_task ON notion_task_sync_state(task_id);
CREATE INDEX IF NOT EXISTS idx_notion_sync_db ON notion_task_sync_state(notion_database_id);
CREATE INDEX IF NOT EXISTS idx_notion_sync_pending ON notion_task_sync_state(last_outbound_revision, last_acknowledged_outbound_revision);
