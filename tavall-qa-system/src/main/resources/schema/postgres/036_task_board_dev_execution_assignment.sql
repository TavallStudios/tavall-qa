-- Allow DEV execution to be granted per task through an assignment, so a qualified
-- Technical QA account (including an orchestrator agent) can lease and checkpoint work.
-- DEV execution assignments are restricted to DEV with the Technical class at Tier III or above.
ALTER TABLE task_board_assignment DROP CONSTRAINT IF EXISTS task_board_assignment_capability_check;
ALTER TABLE task_board_assignment ADD CONSTRAINT task_board_assignment_capability_check CHECK (
    capability IN ('VIEW_QA_TASKS', 'SUBMIT_EVIDENCE', 'REVIEW_EVIDENCE', 'ADD_COMMENT', 'HUMAN_VERIFY',
                   'TECHNICAL_DEV_EXECUTION')
);

ALTER TABLE task_board_assignment DROP CONSTRAINT IF EXISTS task_board_assignment_dev_execution_check;
ALTER TABLE task_board_assignment ADD CONSTRAINT task_board_assignment_dev_execution_check CHECK (
    capability <> 'TECHNICAL_DEV_EXECUTION'
    OR (environment = 'DEV' AND required_qa_class = 'TECHNICAL'
        AND minimum_qa_tier IN ('TIER_III', 'TIER_IV', 'TIER_V'))
);
