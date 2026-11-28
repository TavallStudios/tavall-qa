ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_reviewer_model VARCHAR(128);
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_reviewer_account_id VARCHAR(36);
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_reviewer_agent_identity VARCHAR(255);
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_reviewed_at TIMESTAMPTZ;
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_candidate_key VARCHAR(1);
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_rationale TEXT;
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_evidence_refs TEXT NOT NULL DEFAULT '[]';
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_artifact_refs TEXT NOT NULL DEFAULT '[]';
ALTER TABLE task_board_design_review ADD COLUMN IF NOT EXISTS ai_screenshot_artifact_ref TEXT;
