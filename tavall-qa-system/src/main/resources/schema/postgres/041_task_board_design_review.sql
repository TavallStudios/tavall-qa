-- A task owns one review aggregate; its attributed decision commits with the task transition.
CREATE TABLE IF NOT EXISTS task_board_design_review (
    task_id VARCHAR(36) PRIMARY KEY REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    candidates_json TEXT NOT NULL,
    revision INTEGER NOT NULL CHECK (revision > 0),
    registered_by_account_id VARCHAR(36) NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL,
    winner_key VARCHAR(1),
    decided_by_account_id VARCHAR(36),
    decided_at TIMESTAMPTZ,
    decision_note VARCHAR(4000) NOT NULL DEFAULT '',
    CONSTRAINT task_board_design_candidates_check CHECK (
        jsonb_typeof(candidates_json::jsonb) = 'array'
        AND jsonb_array_length(candidates_json::jsonb) BETWEEN 2 AND 4
    ),
    CONSTRAINT task_board_design_decision_check CHECK (
        (winner_key IS NULL AND decided_by_account_id IS NULL AND decided_at IS NULL AND decision_note = '')
        OR (winner_key IN ('A', 'B', 'C', 'D') AND decided_by_account_id IS NOT NULL AND decided_at IS NOT NULL)
    )
);
