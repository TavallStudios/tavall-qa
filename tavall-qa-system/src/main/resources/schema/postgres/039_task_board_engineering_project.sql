-- Admin-only project for Tavall engineering and AI orchestration work. GitHub Issues stay the canonical
-- engineering task identity; this project holds their QA execution records (leases, checkpoints, status).
INSERT INTO task_board_project (project_key, display_name, description, project_status, audience, sort_order, created_at, updated_at)
VALUES
    ('tavall-engineering', 'Tavall Engineering',
     'Internal execution tracking for Tavall engineering and AI orchestration work linked to GitHub Issues.',
     'ACTIVE', 'ADMIN_ONLY', 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (project_key) DO NOTHING;
