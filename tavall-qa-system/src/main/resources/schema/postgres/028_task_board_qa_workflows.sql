ALTER TABLE task_board_task
    ADD COLUMN IF NOT EXISTS qa_qc_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS qa_revision INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS created_by_account_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS verified_by_account_id VARCHAR(36),
    ADD COLUMN IF NOT EXISTS verified_evidence_id VARCHAR(36);

ALTER TABLE task_board_comment
    ADD COLUMN IF NOT EXISTS author_account_id VARCHAR(36);

ALTER TABLE qa_vqc_contribution
    ADD COLUMN IF NOT EXISTS contributor_account_id VARCHAR(36),
    ALTER COLUMN contributor_minecraft_uuid DROP NOT NULL;

CREATE TABLE IF NOT EXISTS task_board_audit (
    event_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36),
    project_key VARCHAR(96) NOT NULL,
    actor_account_id VARCHAR(36),
    actor_minecraft_uuid VARCHAR(36),
    event_type VARCHAR(40) NOT NULL,
    summary TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_audit_event_check CHECK (event_type IN (
        'PROJECT_CREATED', 'TASK_CREATED', 'TASK_UPDATED', 'TASK_ASSIGNED',
        'TASK_ASSIGNMENT_REVOKED', 'EVIDENCE_SUBMITTED', 'EVIDENCE_ACCEPTED',
        'EVIDENCE_REJECTED', 'TASK_ACCEPTED', 'TASK_HUMAN_VERIFIED', 'TASK_REJECTED',
        'LEGACY_VERIFICATION_SUPERSEDED', 'ACTION_ADDED', 'COMMENT_ADDED'
    )),
    CONSTRAINT task_board_audit_summary_check CHECK (length(btrim(summary)) > 0)
);

INSERT INTO task_board_audit (
    event_id,
    task_id,
    project_key,
    actor_account_id,
    actor_minecraft_uuid,
    event_type,
    summary,
    occurred_at
)
SELECT
    substr(deterministic_id, 1, 8) || '-' ||
        substr(deterministic_id, 9, 4) || '-' ||
        substr(deterministic_id, 13, 4) || '-' ||
        substr(deterministic_id, 17, 4) || '-' ||
        substr(deterministic_id, 21, 12),
    task_id,
    project_key,
    NULL,
    verified_by_minecraft_uuid,
    'LEGACY_VERIFICATION_SUPERSEDED',
    'Legacy HUMAN_VERIFIED state was demoted because it did not reference accepted QA evidence.',
    COALESCE(human_verified_at, updated_at)
FROM (
    SELECT task_id,
           project_key,
           verified_by_minecraft_uuid,
           human_verified_at,
           updated_at,
           md5(task_id || ':legacy-human-verify') AS deterministic_id
    FROM task_board_task
    WHERE qa_status = 'HUMAN_VERIFIED'
) legacy_verification
ON CONFLICT (event_id) DO NOTHING;

UPDATE task_board_task
SET qa_status = 'REVIEW_REQUIRED',
    verified_by_minecraft_uuid = NULL,
    human_verified_at = NULL
WHERE qa_status = 'HUMAN_VERIFIED';

ALTER TABLE task_board_task
    DROP CONSTRAINT IF EXISTS task_board_task_human_verification_check;

CREATE TABLE IF NOT EXISTS task_board_evidence (
    evidence_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    account_id VARCHAR(36) NOT NULL,
    submitter_minecraft_uuid VARCHAR(36),
    qa_classes_at_submission VARCHAR(128) NOT NULL,
    qa_tier_at_submission VARCHAR(16),
    environment VARCHAR(16) NOT NULL,
    build_reference VARCHAR(255) NOT NULL,
    test_case TEXT NOT NULL,
    expected_result TEXT NOT NULL,
    actual_result TEXT NOT NULL,
    evidence_reference VARCHAR(2048) NOT NULL,
    public_summary TEXT NOT NULL DEFAULT '',
    public_summary_approved BOOLEAN NOT NULL DEFAULT FALSE,
    task_fingerprint VARCHAR(64) NOT NULL,
    evidence_status VARCHAR(24) NOT NULL DEFAULT 'PENDING_REVIEW',
    reviewer_account_id VARCHAR(36),
    review_reason TEXT NOT NULL DEFAULT '',
    submitted_at TIMESTAMPTZ NOT NULL,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT task_board_evidence_class_check CHECK (
        qa_classes_at_submission = '' OR qa_classes_at_submission !~ '[^A-Z_,]'
    ),
    CONSTRAINT task_board_evidence_tier_check CHECK (
        qa_tier_at_submission IS NULL OR qa_tier_at_submission IN ('TIER_0', 'TIER_I', 'TIER_II', 'TIER_III', 'TIER_IV', 'TIER_V')
    ),
    CONSTRAINT task_board_evidence_environment_check CHECK (environment IN ('DEV', 'STAGING', 'PROD')),
    CONSTRAINT task_board_evidence_status_check CHECK (evidence_status IN ('PENDING_REVIEW', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT task_board_evidence_review_check CHECK (
        (evidence_status = 'PENDING_REVIEW' AND reviewer_account_id IS NULL AND reviewed_at IS NULL AND NOT public_summary_approved)
        OR (evidence_status <> 'PENDING_REVIEW' AND reviewer_account_id IS NOT NULL AND reviewed_at IS NOT NULL)
    ),
    CONSTRAINT task_board_evidence_public_summary_check CHECK (
        NOT public_summary_approved OR (evidence_status = 'ACCEPTED' AND length(btrim(public_summary)) > 0)
    ),
    CONSTRAINT task_board_evidence_content_check CHECK (
        length(btrim(build_reference)) > 0
        AND length(btrim(test_case)) > 0
        AND length(btrim(expected_result)) > 0
        AND length(btrim(actual_result)) > 0
        AND length(btrim(evidence_reference)) > 0
    )
);

CREATE TABLE IF NOT EXISTS task_board_assignment (
    assignment_id VARCHAR(36) PRIMARY KEY,
    organization_id VARCHAR(36) NOT NULL,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    account_id VARCHAR(36) NOT NULL,
    capability VARCHAR(48) NOT NULL,
    environment VARCHAR(16) NOT NULL,
    required_qa_class VARCHAR(32),
    minimum_qa_tier VARCHAR(16),
    assigned_by_account_id VARCHAR(36) NOT NULL,
    reason TEXT NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    CONSTRAINT task_board_assignment_capability_check CHECK (
        capability IN ('VIEW_QA_TASKS', 'SUBMIT_EVIDENCE', 'REVIEW_EVIDENCE', 'ADD_COMMENT', 'HUMAN_VERIFY')
    ),
    CONSTRAINT task_board_assignment_environment_check CHECK (environment IN ('DEV', 'STAGING', 'PROD')),
    CONSTRAINT task_board_assignment_class_check CHECK (
        required_qa_class IS NULL OR required_qa_class IN ('REGULAR', 'TECHNICAL', 'FOUNDING_PUBLIC')
    ),
    CONSTRAINT task_board_assignment_tier_check CHECK (
        minimum_qa_tier IS NULL OR minimum_qa_tier IN ('TIER_0', 'TIER_I', 'TIER_II', 'TIER_III', 'TIER_IV', 'TIER_V')
    ),
    CONSTRAINT task_board_assignment_reason_check CHECK (length(btrim(reason)) > 0),
    CONSTRAINT task_board_assignment_time_check CHECK (revoked_at IS NULL OR revoked_at >= assigned_at),
    CONSTRAINT task_board_assignment_technical_tier_check CHECK (
        required_qa_class IS DISTINCT FROM 'TECHNICAL'
        OR minimum_qa_tier IN ('TIER_III', 'TIER_IV', 'TIER_V')
    )
);

CREATE INDEX IF NOT EXISTS idx_task_board_evidence_task_status
    ON task_board_evidence(task_id, evidence_status, submitted_at);
CREATE INDEX IF NOT EXISTS idx_task_board_assignment_account_active
    ON task_board_assignment(account_id, revoked_at, assigned_at);
CREATE INDEX IF NOT EXISTS idx_task_board_assignment_task_active
    ON task_board_assignment(task_id, revoked_at, assigned_at);
CREATE INDEX IF NOT EXISTS idx_task_board_audit_task_time
    ON task_board_audit(task_id, occurred_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_task_board_assignment_active_scope
    ON task_board_assignment(task_id, account_id, capability, environment)
    WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_qa_vqc_contribution_task_account
    ON qa_vqc_contribution(task_id, contributor_account_id)
    WHERE contributor_account_id IS NOT NULL;

ALTER TABLE task_board_task
    ADD CONSTRAINT task_board_task_verified_evidence_fk
        FOREIGN KEY (verified_evidence_id) REFERENCES task_board_evidence(evidence_id),
    ADD CONSTRAINT task_board_task_human_verification_check CHECK (
        (qa_status = 'HUMAN_VERIFIED'
            AND verified_by_account_id IS NOT NULL
            AND verified_by_minecraft_uuid IS NOT NULL
            AND verified_evidence_id IS NOT NULL
            AND human_verified_at IS NOT NULL)
        OR
        (qa_status <> 'HUMAN_VERIFIED'
            AND verified_by_account_id IS NULL
            AND verified_by_minecraft_uuid IS NULL
            AND verified_evidence_id IS NULL
            AND human_verified_at IS NULL)
    );

CREATE OR REPLACE FUNCTION task_board_audit_reject_row_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'task_board_audit is append-only';
END;
$$;

CREATE OR REPLACE FUNCTION task_board_audit_reject_truncate()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'task_board_audit is append-only';
END;
$$;

DROP TRIGGER IF EXISTS task_board_audit_no_row_mutation ON task_board_audit;
CREATE TRIGGER task_board_audit_no_row_mutation
    BEFORE UPDATE OR DELETE ON task_board_audit
    FOR EACH ROW EXECUTE FUNCTION task_board_audit_reject_row_mutation();

DROP TRIGGER IF EXISTS task_board_audit_no_truncate ON task_board_audit;
CREATE TRIGGER task_board_audit_no_truncate
    BEFORE TRUNCATE ON task_board_audit
    FOR EACH STATEMENT EXECUTE FUNCTION task_board_audit_reject_truncate();
