CREATE TABLE IF NOT EXISTS task_board_external_reference_alias (
    alias_key VARCHAR(255) PRIMARY KEY,
    reference_id VARCHAR(36) NOT NULL REFERENCES task_board_external_reference(reference_id)
);

CREATE INDEX IF NOT EXISTS idx_task_board_external_reference_alias_reference
    ON task_board_external_reference_alias(reference_id);

CREATE INDEX IF NOT EXISTS idx_task_board_task_external_key_lower
    ON task_board_task (lower(external_key));

-- Existing URLs remain available after the first rename. Ties are left for owner review.
INSERT INTO task_board_external_reference_alias (alias_key, reference_id)
SELECT 'github/' || substring(lower(source_url) FROM 20), min(reference_id)
FROM task_board_external_reference
WHERE provider = 'GITHUB_ISSUE'
  AND lower(source_url) ~ '^https://github\.com/[a-z0-9_.-]+/[a-z0-9_.-]+/issues/[1-9][0-9]*$'
  AND length(source_url) <= 267
GROUP BY lower(source_url)
HAVING count(*) = 1
ON CONFLICT (alias_key) DO NOTHING;
