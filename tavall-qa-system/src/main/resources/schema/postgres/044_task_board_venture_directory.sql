-- Migration 042 introduced the directory and removed the 040 projects. Attach source-card
-- metadata to that directory, including cards that were not in the original 28-entry catalog.
ALTER TABLE task_board_venture ADD COLUMN IF NOT EXISTS display_name TEXT;
ALTER TABLE task_board_venture ADD COLUMN IF NOT EXISTS source_task_id VARCHAR(36)
    REFERENCES task_board_task(task_id) ON DELETE SET NULL;
ALTER TABLE task_board_venture ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE task_board_venture ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Retain the original 040 display names and recorded URLs after the project rows are retired.
WITH catalog(slug, display_name, live_urls_json) AS (VALUES
    ('tavall-web', 'Tavall Web', '["https://tavall.org/"]'),
    ('tavall-times-blog', 'Tavall Times Blog', '["https://blog.tavall.org/"]'),
    ('tavall-account', 'Tavall Account', '["https://account.tavall.org/"]'),
    ('tavall-dashboard', 'Tavall Dashboard', '[]'),
    ('tavall-commerce', 'Tavall Commerce', '["https://commerce.tavall.org/"]'),
    ('tavall-docs', 'Tavall Docs', '["https://docs.tavall.org/"]'),
    ('tavall-qa', 'Tavall QA Board', '["https://qa.tavall.org/"]'),
    ('tavall-content', 'Tavall Content', '["https://content.tavall.org/"]'),
    ('tavall-cloud', 'Tavall Cloud', '[]'),
    ('tavall-ai', 'Tavall AI', '[]'),
    ('tavall-ai-chat', 'Tavall AI Chat', '["https://ai.tavall.org/"]'),
    ('tavall-ai-plugin', 'Tavall AI Plugin', '[]'),
    ('tavall-mcp', 'Tavall MCP', '["https://mcp.tavall.org/"]'),
    ('tavall-analytics', 'Tavall Analytics', '["https://analytics.tavall.org/"]'),
    ('tavall-workflows', 'Tavall Workflows', '["https://workflows.tavall.org/"]'),
    ('tavall-contractors', 'Tavall Contractors', '[]'),
    ('tavall-couriers', 'Tavall Couriers', '[]'),
    ('tavall-builder', 'Tavall Builder', '[]'),
    ('tavall-affiliates', 'Tavall Affiliates', '[]'),
    ('tavall-labs', 'Tavall Labs', '[]'),
    ('tavall-ops', 'Tavall Ops', '[]'),
    ('tavall-mc-speedrun', 'Tavall MC Speedrun', '[]'),
    ('tavall-webstore', 'Tavall Webstore', '[]'),
    ('tavall-hytale', 'Tavall Hytale', '[]'),
    ('tavall-roblox', 'Tavall Roblox', '[]'),
    ('tavall-open-harness', 'Tavall Open Harness', '[]'),
    ('tavall-novus', 'Project Novus', '[]'),
    ('tavall-pvp', 'Tavall PvP', '[]')
)
UPDATE task_board_venture AS venture
SET display_name = catalog.display_name,
    live_urls_json = CASE WHEN venture.live_urls_json = '[]' THEN catalog.live_urls_json ELSE venture.live_urls_json END
FROM catalog
WHERE venture.slug = catalog.slug AND venture.display_name IS NULL;

WITH cards AS (
    SELECT split_part(external_key, '/', 2) AS slug, task_id, external_key, title, description,
           created_at, updated_at
    FROM task_board_task
    WHERE external_key ~ '^venture/[a-z0-9][a-z0-9-]{0,95}/venture-card$'
)
INSERT INTO task_board_venture AS venture (
    slug, display_name, source_task_key, source_task_id, purpose, owner_repos_json,
    stage, live_urls_json, top_blocker, created_at, updated_at
)
SELECT cards.slug, COALESCE(known.display_name, NULLIF(btrim(cards.title), ''), cards.slug),
       cards.external_key, cards.task_id,
       COALESCE(NULLIF(btrim(substring(cards.description FROM 'Purpose: (.*?)\. Top blocker:')), ''),
                known.purpose, NULLIF(btrim(cards.description), ''), cards.title),
       COALESCE(known.owner_repos_json, '[]'),
       COALESCE(NULLIF(btrim(substring(cards.description FROM '^Venture venture-card for .*? [(]([^)]*)[)]\. Purpose:')), ''),
                known.stage, 'UNRECORDED'),
       COALESCE(known.live_urls_json, '[]'),
       COALESCE(NULLIF(btrim(substring(cards.description FROM 'Top blocker: (.*?)\. Source:')), ''),
                known.top_blocker, 'Unrecorded'),
       cards.created_at, cards.updated_at
FROM cards
LEFT JOIN task_board_venture AS known ON known.slug = cards.slug
ON CONFLICT (slug) DO UPDATE
SET source_task_id = EXCLUDED.source_task_id, purpose = EXCLUDED.purpose,
    stage = EXCLUDED.stage, top_blocker = EXCLUDED.top_blocker,
    created_at = EXCLUDED.created_at, updated_at = EXCLUDED.updated_at
WHERE venture.source_task_id IS NULL;

UPDATE task_board_venture SET display_name = slug WHERE display_name IS NULL;
ALTER TABLE task_board_venture ALTER COLUMN display_name SET NOT NULL;

-- The task FK cascades on project deletion. Serialize board writes across the move
-- and retirement so a concurrent insert cannot be deleted with its old project.
DO $$
DECLARE
    retired_projects CONSTANT TEXT[] := ARRAY[
        'tavall-web', 'tavall-times-blog', 'tavall-account', 'tavall-dashboard',
        'tavall-commerce', 'tavall-docs', 'tavall-qa', 'tavall-content',
        'tavall-cloud', 'tavall-ai', 'tavall-ai-chat', 'tavall-ai-plugin',
        'tavall-mcp', 'tavall-analytics', 'tavall-workflows', 'tavall-contractors',
        'tavall-couriers', 'tavall-builder', 'tavall-affiliates', 'tavall-labs',
        'tavall-ops', 'tavall-mc-speedrun', 'tavall-webstore', 'tavall-hytale',
        'tavall-roblox', 'tavall-open-harness'
    ];
BEGIN
    LOCK TABLE task_board_task IN SHARE ROW EXCLUSIVE MODE;

    UPDATE task_board_task AS task
    SET project_key = 'tavall-engineering'
    WHERE task.project_key = ANY (retired_projects)
       OR (task.external_key, task.project_key) IN (VALUES
           ('venture/tavall-novus/venture-card', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/401', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/399', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/398', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/397', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/394', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/393', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/392', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/386', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/360', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/347', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/306', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/293', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/263', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/255', 'project-novus'),
           ('github/TavallStudios/tavall-mc/issues/201', 'project-novus'),
           ('github/TavallStudios/tavall-web-mc/issues/11', 'project-novus'),
           ('github/TavallStudios/tavall-minecraft-framework/issues/19', 'project-novus'),
           ('github/TavallStudios/tavall-minecraft-framework/issues/10', 'project-novus'),
           ('github/TavallStudios/tavall-mc-anticheat/issues/4', 'project-novus'),
           ('github/TavallStudios/tavall-mc-packet-filter/issues/4', 'project-novus'),
           ('github/TavallStudios/tavall-mc-bot-testing/issues/1', 'project-novus'),
           ('venture/tavall-pvp/venture-card', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/401', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/399', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/398', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/397', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/394', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/393', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/392', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/386', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/360', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/347', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/306', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/293', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/263', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/255', 'tavall-pvp'),
           ('github/TavallStudios/tavall-mc/issues/201', 'tavall-pvp')
       );

    DELETE FROM task_board_project WHERE project_key = ANY (retired_projects);
END $$;
