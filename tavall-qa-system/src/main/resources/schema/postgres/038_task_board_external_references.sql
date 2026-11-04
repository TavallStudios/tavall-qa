CREATE TABLE IF NOT EXISTS task_board_external_reference (
    reference_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id),
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('GITHUB_ISSUE', 'LINEAR_ISSUE')),
    source_namespace_id VARCHAR(255) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    display_key VARCHAR(255) NOT NULL,
    source_url VARCHAR(2048) NOT NULL,
    source_status VARCHAR(32) NOT NULL CHECK (source_status IN (
        'UNKNOWN', 'OPEN', 'READY', 'ACTIVE', 'IN_PROGRESS', 'BLOCKED', 'DONE',
        'COMPLETED', 'CLOSED', 'CANCELLED', 'ARCHIVED'
    )),
    last_observed_revision VARCHAR(255) NOT NULL DEFAULT '',
    last_applied_revision VARCHAR(255) NOT NULL DEFAULT '',
    last_observed_cursor VARCHAR(512) NOT NULL DEFAULT '',
    source_fingerprint VARCHAR(128) NOT NULL DEFAULT '',
    sync_state VARCHAR(32) NOT NULL CHECK (sync_state IN (
        'CONNECTED', 'PENDING', 'CONFLICT', 'ACTION_REQUIRED', 'FAILED'
    )),
    sync_revision BIGINT NOT NULL CHECK (sync_revision > 0),
    last_observed_at TIMESTAMPTZ NOT NULL,
    last_applied_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_task_board_external_reference_fingerprint CHECK (
        source_fingerprint = '' OR source_fingerprint ~ '^[A-Fa-f0-9]{64}$'
    ),
    CONSTRAINT uq_task_board_external_reference_source UNIQUE (provider, source_namespace_id, source_id),
    CONSTRAINT uq_task_board_external_reference_task_provider UNIQUE (task_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_task_board_external_reference_task
    ON task_board_external_reference(task_id, provider);

CREATE TABLE IF NOT EXISTS task_board_external_reference_conflict (
    conflict_id VARCHAR(36) PRIMARY KEY,
    provider VARCHAR(32) NOT NULL CHECK (provider IN ('GITHUB_ISSUE', 'LINEAR_ISSUE')),
    source_namespace_id VARCHAR(255) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    reference_id VARCHAR(36) REFERENCES task_board_external_reference(reference_id),
    existing_task_id VARCHAR(36) REFERENCES task_board_task(task_id),
    requested_task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id),
    incoming_display_key VARCHAR(255) NOT NULL,
    incoming_source_url VARCHAR(2048) NOT NULL,
    incoming_source_status VARCHAR(32) NOT NULL CHECK (incoming_source_status IN (
        'UNKNOWN', 'OPEN', 'READY', 'ACTIVE', 'IN_PROGRESS', 'BLOCKED', 'DONE',
        'COMPLETED', 'CLOSED', 'CANCELLED', 'ARCHIVED'
    )),
    incoming_source_revision VARCHAR(255) NOT NULL,
    incoming_source_cursor VARCHAR(512) NOT NULL DEFAULT '',
    incoming_source_fingerprint VARCHAR(128) NOT NULL,
    incoming_observed_at TIMESTAMPTZ NOT NULL,
    expected_sync_revision BIGINT NOT NULL CHECK (expected_sync_revision >= 0),
    reason TEXT NOT NULL CHECK (length(btrim(reason)) > 0),
    status VARCHAR(24) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    resolution VARCHAR(24) CHECK (resolution IN ('KEEP_CURRENT', 'APPLY_INCOMING')),
    resolved_by_account_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT ck_task_board_external_reference_conflict_fingerprint CHECK (
        incoming_source_fingerprint ~ '^[A-Fa-f0-9]{64}$'
    ),
    CONSTRAINT ck_task_board_ext_ref_conflict_state_resolution CHECK (
        (status = 'OPEN' AND resolution IS NULL AND resolved_at IS NULL)
        OR (status = 'RESOLVED' AND resolution IS NOT NULL AND resolved_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_task_board_external_reference_conflict_queue
    ON task_board_external_reference_conflict(status, created_at);
CREATE INDEX IF NOT EXISTS idx_task_board_external_reference_conflict_task
    ON task_board_external_reference_conflict(requested_task_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_task_board_external_reference_conflict_source
    ON task_board_external_reference_conflict(provider, source_namespace_id, source_id, incoming_source_revision);

ALTER TABLE task_board_audit
    DROP CONSTRAINT IF EXISTS task_board_audit_event_check;

ALTER TABLE task_board_audit
    ADD CONSTRAINT task_board_audit_event_check CHECK (event_type IN (
        'PROJECT_CREATED', 'TASK_CREATED', 'TASK_UPDATED', 'TASK_UPSERTED', 'TASK_ASSIGNED',
        'TASK_ASSIGNMENT_REVOKED', 'EVIDENCE_SUBMITTED', 'EVIDENCE_ACCEPTED',
        'EVIDENCE_REJECTED', 'TASK_ACCEPTED', 'TASK_HUMAN_VERIFIED', 'TASK_REJECTED',
        'LEGACY_VERIFICATION_SUPERSEDED', 'ACTION_ADDED', 'COMMENT_ADDED',
        'EXECUTION_LEASE_ACQUIRED', 'EXECUTION_LEASE_RENEWED', 'EXECUTION_LEASE_RELEASED',
        'EXECUTION_CHECKPOINT_RECORDED', 'EXTERNAL_REFERENCE_UPSERTED',
        'EXTERNAL_REFERENCE_CONFLICTED', 'EXTERNAL_REFERENCE_RESOLVED'
    ));
