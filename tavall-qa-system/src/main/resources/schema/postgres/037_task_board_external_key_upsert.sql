ALTER TABLE task_board_task
    ADD COLUMN IF NOT EXISTS external_key VARCHAR(255);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uq_task_board_task_external_key'
          AND conrelid = 'task_board_task'::regclass
    ) THEN
        ALTER TABLE task_board_task
            ADD CONSTRAINT uq_task_board_task_external_key UNIQUE (external_key);
    END IF;
END $$;

ALTER TABLE task_board_audit
    DROP CONSTRAINT IF EXISTS task_board_audit_event_check;

ALTER TABLE task_board_audit
    ADD CONSTRAINT task_board_audit_event_check CHECK (event_type IN (
        'PROJECT_CREATED', 'TASK_CREATED', 'TASK_UPDATED', 'TASK_UPSERTED', 'TASK_ASSIGNED',
        'TASK_ASSIGNMENT_REVOKED', 'EVIDENCE_SUBMITTED', 'EVIDENCE_ACCEPTED',
        'EVIDENCE_REJECTED', 'TASK_ACCEPTED', 'TASK_HUMAN_VERIFIED', 'TASK_REJECTED',
        'LEGACY_VERIFICATION_SUPERSEDED', 'ACTION_ADDED', 'COMMENT_ADDED',
        'EXECUTION_LEASE_ACQUIRED', 'EXECUTION_LEASE_RENEWED', 'EXECUTION_LEASE_RELEASED',
        'EXECUTION_CHECKPOINT_RECORDED'
    ));
