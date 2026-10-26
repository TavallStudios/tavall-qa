CREATE TABLE IF NOT EXISTS task_board_execution_lease_token (
    lease_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE RESTRICT,
    run_id VARCHAR(36) NOT NULL,
    owner_account_id VARCHAR(36) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    initial_expires_at TIMESTAMPTZ NOT NULL CHECK (initial_expires_at > issued_at)
);

CREATE TABLE IF NOT EXISTS task_board_execution_lease (
    task_id VARCHAR(36) PRIMARY KEY REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    run_id VARCHAR(36) NOT NULL,
    lease_id VARCHAR(36) NOT NULL UNIQUE REFERENCES task_board_execution_lease_token(lease_id)
        DEFERRABLE INITIALLY DEFERRED,
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
    CONSTRAINT task_board_execution_checkpoint_sequence UNIQUE (task_id, run_id, checkpoint_sequence),
    CONSTRAINT task_board_execution_checkpoint_digest UNIQUE (checkpoint_id, payload_sha256)
);

CREATE TABLE IF NOT EXISTS task_board_execution_checkpoint_payload (
    checkpoint_id VARCHAR(36) PRIMARY KEY,
    payload_sha256 VARCHAR(64) NOT NULL CHECK (payload_sha256 ~ '^[a-f0-9]{64}$'),
    payload_json TEXT NOT NULL CHECK (octet_length(payload_json) <= 1048576),
    CONSTRAINT task_board_execution_payload_digest UNIQUE (checkpoint_id, payload_sha256),
    CONSTRAINT task_board_execution_payload_checkpoint FOREIGN KEY (checkpoint_id, payload_sha256)
        REFERENCES task_board_execution_checkpoint(checkpoint_id, payload_sha256)
        ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED
);

ALTER TABLE task_board_execution_checkpoint DROP CONSTRAINT IF EXISTS task_board_execution_checkpoint_payload;
ALTER TABLE task_board_execution_checkpoint ADD CONSTRAINT task_board_execution_checkpoint_payload
    FOREIGN KEY (checkpoint_id, payload_sha256)
    REFERENCES task_board_execution_checkpoint_payload(checkpoint_id, payload_sha256)
    ON DELETE RESTRICT DEFERRABLE INITIALLY DEFERRED;

CREATE OR REPLACE FUNCTION task_board_reject_checkpoint_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Task execution checkpoint history is append-only';
END;
$$;

DROP TRIGGER IF EXISTS task_board_execution_token_no_row_mutation ON task_board_execution_lease_token;
CREATE TRIGGER task_board_execution_token_no_row_mutation
BEFORE UPDATE OR DELETE ON task_board_execution_lease_token
FOR EACH ROW EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

DROP TRIGGER IF EXISTS task_board_execution_token_no_truncate ON task_board_execution_lease_token;
CREATE TRIGGER task_board_execution_token_no_truncate
BEFORE TRUNCATE ON task_board_execution_lease_token
FOR EACH STATEMENT EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

DROP TRIGGER IF EXISTS task_board_checkpoint_no_row_mutation ON task_board_execution_checkpoint;
CREATE TRIGGER task_board_checkpoint_no_row_mutation
BEFORE UPDATE OR DELETE ON task_board_execution_checkpoint
FOR EACH ROW EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

DROP TRIGGER IF EXISTS task_board_checkpoint_no_truncate ON task_board_execution_checkpoint;
CREATE TRIGGER task_board_checkpoint_no_truncate
BEFORE TRUNCATE ON task_board_execution_checkpoint
FOR EACH STATEMENT EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

DROP TRIGGER IF EXISTS task_board_checkpoint_payload_no_row_mutation ON task_board_execution_checkpoint_payload;
CREATE TRIGGER task_board_checkpoint_payload_no_row_mutation
BEFORE UPDATE OR DELETE ON task_board_execution_checkpoint_payload
FOR EACH ROW EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

DROP TRIGGER IF EXISTS task_board_checkpoint_payload_no_truncate ON task_board_execution_checkpoint_payload;
CREATE TRIGGER task_board_checkpoint_payload_no_truncate
BEFORE TRUNCATE ON task_board_execution_checkpoint_payload
FOR EACH STATEMENT EXECUTE FUNCTION task_board_reject_checkpoint_mutation();

ALTER TABLE task_board_audit DROP CONSTRAINT IF EXISTS task_board_audit_event_check;
ALTER TABLE task_board_audit ADD CONSTRAINT task_board_audit_event_check CHECK (event_type IN (
    'PROJECT_CREATED', 'TASK_CREATED', 'TASK_UPDATED', 'TASK_ASSIGNED', 'TASK_ASSIGNMENT_REVOKED',
    'EVIDENCE_SUBMITTED', 'EVIDENCE_ACCEPTED', 'EVIDENCE_REJECTED', 'TASK_ACCEPTED', 'TASK_HUMAN_VERIFIED',
    'TASK_REJECTED', 'LEGACY_VERIFICATION_SUPERSEDED', 'ACTION_ADDED', 'COMMENT_ADDED',
    'EXECUTION_LEASE_ACQUIRED', 'EXECUTION_LEASE_RENEWED', 'EXECUTION_LEASE_RELEASED', 'EXECUTION_CHECKPOINT_RECORDED'
));
