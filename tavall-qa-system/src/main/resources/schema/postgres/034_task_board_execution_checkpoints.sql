CREATE TABLE IF NOT EXISTS task_board_execution_lease (
    task_id VARCHAR(36) PRIMARY KEY REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    run_id VARCHAR(36) NOT NULL,
    lease_id VARCHAR(36) NOT NULL UNIQUE,
    owner_account_id VARCHAR(36) NOT NULL,
    revision BIGINT NOT NULL CHECK (revision >= 0),
    checkpoint_sequence BIGINT NOT NULL CHECK (checkpoint_sequence >= 0),
    task_fingerprint VARCHAR(64) NOT NULL CHECK (task_fingerprint ~ '^[a-f0-9]{64}$'),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS task_board_execution_checkpoint (
    checkpoint_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE RESTRICT,
    run_id VARCHAR(36) NOT NULL,
    lease_id VARCHAR(36) NOT NULL,
    checkpoint_sequence BIGINT NOT NULL CHECK (checkpoint_sequence >= 1),
    task_fingerprint VARCHAR(64) NOT NULL CHECK (task_fingerprint ~ '^[a-f0-9]{64}$'),
    source_repository VARCHAR(201) NOT NULL,
    source_branch VARCHAR(256) NOT NULL CHECK (length(btrim(source_branch)) > 0),
    source_commit VARCHAR(40) NOT NULL CHECK (source_commit ~ '^[a-f0-9]{40}$'),
    diff_sha256 VARCHAR(64) NOT NULL CHECK (diff_sha256 ~ '^[a-f0-9]{64}$'),
    payload_sha256 VARCHAR(64) NOT NULL CHECK (payload_sha256 ~ '^[a-f0-9]{64}$'),
    provider_id VARCHAR(96) NOT NULL CHECK (provider_id ~ '^[a-z0-9][a-z0-9._-]{0,95}$'),
    execution_status VARCHAR(32) NOT NULL CHECK (execution_status IN (
        'QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'TIMED_OUT', 'CANCELLED', 'BLOCKED', 'INFRASTRUCTURE_FAILURE'
    )),
    recorded_by_account_id VARCHAR(36) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_execution_checkpoint_sequence UNIQUE (task_id, run_id, checkpoint_sequence)
);

ALTER TABLE task_board_audit DROP CONSTRAINT IF EXISTS task_board_audit_event_check;
ALTER TABLE task_board_audit ADD CONSTRAINT task_board_audit_event_check CHECK (event_type IN (
    'PROJECT_CREATED', 'TASK_CREATED', 'TASK_UPDATED', 'TASK_ASSIGNED', 'TASK_ASSIGNMENT_REVOKED',
    'EVIDENCE_SUBMITTED', 'EVIDENCE_ACCEPTED', 'EVIDENCE_REJECTED', 'TASK_ACCEPTED', 'TASK_HUMAN_VERIFIED',
    'TASK_REJECTED', 'LEGACY_VERIFICATION_SUPERSEDED', 'ACTION_ADDED', 'COMMENT_ADDED',
    'EXECUTION_LEASE_ACQUIRED', 'EXECUTION_LEASE_RENEWED', 'EXECUTION_LEASE_RELEASED', 'EXECUTION_CHECKPOINT_RECORDED'
));
