-- Restore venture-card and re-homed tasks to the engineering board while creating a separate venture directory.
CREATE TABLE IF NOT EXISTS task_board_venture (
    slug VARCHAR(96) PRIMARY KEY,
    source_task_key VARCHAR(255) NOT NULL UNIQUE,
    purpose TEXT NOT NULL,
    owner_repos_json TEXT NOT NULL,
    stage VARCHAR(96) NOT NULL,
    live_urls_json TEXT NOT NULL DEFAULT '[]',
    top_blocker TEXT NOT NULL,
    goal_family_ref VARCHAR(255)
);

INSERT INTO task_board_venture (slug, source_task_key, purpose, owner_repos_json, stage, live_urls_json, top_blocker, goal_family_ref)
VALUES
    ('tavall-web', 'venture/tavall-web/venture-card', 'Core service router + company home, deals, releases', '["tavall-web"]', 'LIVE', '[]', 'Operator /admin/* routes gating (#137)', NULL),
    ('tavall-times-blog', 'venture/tavall-times-blog/venture-card', 'Tavall Times editorial blog (Deals/Releases)', '["tavall-web-blog"]', 'LIVE', '[]', 'Content clipping pipeline (#10/#101)', NULL),
    ('tavall-account', 'venture/tavall-account/venture-card', 'Identity authority / OAuth2 OIDC auth server', '["tavall-web-account"]', 'LIVE', '[]', 'Social-connection -> payout linkage (#59)', NULL),
    ('tavall-dashboard', 'venture/tavall-dashboard/venture-card', 'Creator & user dashboard', '["tavall-web-dashboard"]', 'LIVE', '[]', 'Account SSO wiring completeness', NULL),
    ('tavall-commerce', 'venture/tavall-commerce/venture-card', 'Commerce, store, settlement, payouts', '["tavall-commerce","tavall-web-commerce","tavall-web-store-tools","tavall-pay","tavall-pay-web","tavall-pay-mc"]', 'LIVE', '[]', 'Payout entitlement correctness (#76); overlaps Webstore', NULL),
    ('tavall-docs', 'venture/tavall-docs/venture-card', 'Public documentation viewer + authority', '["tavall-docs","tavall-web-docs","tavall-docs-private"]', 'LIVE', '[]', 'Local docs index drift (28c81b1 vs 9e2b1a99)', NULL),
    ('tavall-qa', 'venture/tavall-qa/venture-card', 'QA Task Board authority + public view', '["tavall-qa","tavall-web-qa"]', 'LIVE', '[]', 'Venture fields modeled (migration 040, this worker)', NULL),
    ('tavall-content', 'venture/tavall-content/venture-card', 'Content studio backend + clipping surface', '["tavall-content","tavall-web-content","tavall-content-tools","tavall-content-personal"]', 'LIVE', '[]', 'Account YouTube/TikTok connection -> payout (#59/#76)', NULL),
    ('tavall-cloud', 'venture/tavall-cloud/venture-card', 'MCP-first control plane, agent, data, cluster', '["tavall-cloud","tavall-cli","tavall-cloud-networking"]', 'OPERATIONAL', '[]', 'Node enrollment isolation (#504)', NULL),
    ('tavall-ai', 'venture/tavall-ai/venture-card', 'AI platform: agents, memory, chat backend', '["tavall-ai","tavall-ai-memory"]', 'BUILDING/LIVE', '[]', 'Model cost/throughput', NULL),
    ('tavall-ai-chat', 'venture/tavall-ai-chat/venture-card', 'AI chat user surface', '["tavall-ai-chat","tavall-web-ai-chat"]', 'LIVE', '[]', 'See tavall-ai', NULL),
    ('tavall-ai-plugin', 'venture/tavall-ai-plugin/venture-card', 'Tavall AI plugin / agent harness (this runtime)', '["tavall-ai"]', 'LIVE (internal)', '[]', 'Plugin deploy prohibited by policy', NULL),
    ('tavall-mcp', 'venture/tavall-mcp/venture-card', 'Web MCP transport', '["tavall-web-mcp"]', 'LIVE', '[]', 'Tool catalog coverage', NULL),
    ('tavall-analytics', 'venture/tavall-analytics/venture-card', 'Product/user funnel analytics', '["tavall-analytics"]', 'LIVE', '[]', 'Event ingestion coverage', NULL),
    ('tavall-workflows', 'venture/tavall-workflows/venture-card', 'Workflow automation surface', '["tavall-workflows","tavall-web-workflows"]', 'LIVE', '[]', 'Workflow engine completeness', NULL),
    ('tavall-contractors', 'venture/tavall-contractors/venture-card', 'Contractors & talent platform', '["TavallContractors","Tavall-Talent-and-Contractors-","tavall-web-contractors"]', 'LIVE', '[]', 'Talent onboarding flow', NULL),
    ('tavall-couriers', 'venture/tavall-couriers/venture-card', 'Couriers platform (Spring webview)', '["TavallCouriers"]', 'BUILDING', '[]', 'Unvalidated demand / product scope', NULL),
    ('tavall-novus', 'venture/tavall-novus/venture-card', 'Project Novus Minecraft server/world', '["tavall-mc","tavall-project-novus","tavall-mc-studio","tavall-web-mc","tavall-minecraft-framework","tavall-mc-paper","tavall-mc-anticheat","tavall-mc-packet-filter","tavall-mc-bot-testing","CustomMinecraftServer","minecraft-bot"]', 'LIVE', '[]', 'Anticheat/packet-filter hardening', NULL),
    ('tavall-pvp', 'venture/tavall-pvp/venture-card', 'Tavall PvP game mode', '["tavall-mc"]', 'LIVE', '[]', 'Gameplay balance', NULL),
    ('tavall-builder', 'venture/tavall-builder/venture-card', 'Builder Studio (site/content builder tool)', '[]', 'BUILDING/IDEA', '[]', 'Scope undefined (repo unknown)', NULL),
    ('tavall-affiliates', 'venture/tavall-affiliates/venture-card', 'Affiliate marketing', '["tavall-affiliates","tavall-web-affiliates","tavall-affiliate-marketing"]', 'BUILDING', '[]', 'Tracking/settlement', NULL),
    ('tavall-labs', 'venture/tavall-labs/venture-card', 'Labs / Biz experiments', '["tavall-labs","tavall-web-labs","tavall-biz","tavall-web-biz","tavall-orgs"]', 'IDEA/BUILDING', '[]', 'Prioritization', NULL),
    ('tavall-ops', 'venture/tavall-ops/venture-card', 'Private operations control plane', '["tavall-web-ops"]', 'OPERATIONAL', '[]', 'Production target promotion gating', NULL),
    ('tavall-mc-speedrun', 'venture/tavall-mc-speedrun/venture-card', 'Minecraft speedrun platform', '["MCRSpeedrun"]', 'BUILDING', '[]', 'Scope', NULL),
    ('tavall-webstore', 'venture/tavall-webstore/venture-card', 'Multi-tenant commerce platform', '["Webstore","tavall-web-store-tools"]', 'BUILDING', '[]', 'Overlaps tavall-commerce', NULL),
    ('tavall-hytale', 'venture/tavall-hytale/venture-card', 'Hytale ventures (HyRhythm, resource game)', '["HyRhythm","tavall-hytale-resource-game","hytale-bots"]', 'BUILDING/IDEA', '[]', 'Hytale engine status uncertainty', NULL),
    ('tavall-roblox', 'venture/tavall-roblox/venture-card', 'Roblox presence', '["tavall-roblox"]', 'IDEA', '[]', 'Scope', NULL),
    ('tavall-open-harness', 'venture/tavall-open-harness/venture-card', 'CI / open harness', '["tavall-open-harness","tavall-ci"]', 'OPERATIONAL', '[]', 'CI gate diagnostics depth', NULL)
ON CONFLICT (slug) DO NOTHING;

-- The source file contains 197 attempts: 196 successes, one failed content#72 upsert, and 165
-- distinct successful external task keys. Repeated keys moved between destinations; update once.
UPDATE task_board_task
SET project_key = 'tavall-engineering'
WHERE external_key IN (
    'venture/tavall-web/venture-card', 'github/TavallStudios/tavall-web/issues/106', 'github/TavallStudios/tavall-web/issues/105', 'github/TavallStudios/tavall-web/issues/104',
    'github/TavallStudios/tavall-web/issues/103', 'github/TavallStudios/tavall-web/issues/101', 'github/TavallStudios/tavall-web/issues/92', 'github/TavallStudios/tavall-web/issues/89',
    'github/TavallStudios/tavall-web/issues/77', 'github/TavallStudios/tavall-web/issues/65', 'venture/tavall-times-blog/venture-card', 'github/TavallStudios/tavall-web-blog/issues/13',
    'github/TavallStudios/tavall-web-blog/issues/11', 'github/TavallStudios/tavall-web-blog/issues/10', 'venture/tavall-account/venture-card', 'github/TavallStudios/tavall-web-account/issues/7',
    'venture/tavall-dashboard/venture-card', 'github/TavallStudios/tavall-web-dashboard/issues/10', 'github/TavallStudios/tavall-web-dashboard/issues/8', 'github/TavallStudios/tavall-web-dashboard/issues/7',
    'venture/tavall-commerce/venture-card', 'github/TavallStudios/tavall-commerce/issues/3', 'github/TavallStudios/tavall-commerce/issues/2', 'github/TavallStudios/tavall-web-commerce/issues/6',
    'github/TavallStudios/tavall-web-store-tools/issues/4', 'github/TavallStudios/tavall-pay/issues/7', 'github/TavallStudios/tavall-pay/issues/2', 'github/TavallStudios/tavall-pay-web/issues/4',
    'github/TavallStudios/tavall-pay-mc/issues/2', 'venture/tavall-docs/venture-card', 'github/TavallStudios/tavall-docs/issues/107', 'github/TavallStudios/tavall-docs/issues/101',
    'github/TavallStudios/tavall-docs/issues/100', 'github/TavallStudios/tavall-docs/issues/99', 'github/TavallStudios/tavall-docs/issues/98', 'github/TavallStudios/tavall-docs/issues/78',
    'github/TavallStudios/tavall-docs/issues/16', 'github/TavallStudios/tavall-web-docs/issues/3', 'venture/tavall-qa/venture-card', 'github/TavallStudios/tavall-qa/issues/15',
    'github/TavallStudios/tavall-qa/issues/9', 'github/TavallStudios/tavall-qa/issues/6', 'github/TavallStudios/tavall-web-qa/issues/5', 'venture/tavall-content/venture-card',
    'github/TavallStudios/tavall-content/issues/85', 'github/TavallStudios/tavall-content/issues/82', 'github/TavallStudios/tavall-content/issues/81', 'github/TavallStudios/tavall-content/issues/80',
    'github/TavallStudios/tavall-content/issues/78', 'github/TavallStudios/tavall-content/issues/76', 'github/TavallStudios/tavall-content/issues/59', 'github/TavallStudios/tavall-content/issues/5',
    'github/TavallStudios/tavall-content/issues/4', 'github/TavallStudios/tavall-web-content/issues/20', 'github/TavallStudios/tavall-web-content/issues/19', 'venture/tavall-cloud/venture-card',
    'github/TavallStudios/tavall-cloud/issues/504', 'github/TavallStudios/tavall-cloud/issues/502', 'github/TavallStudios/tavall-cloud/issues/500', 'github/TavallStudios/tavall-cloud/issues/499',
    'github/TavallStudios/tavall-cloud/issues/497', 'github/TavallStudios/tavall-cloud/issues/496', 'github/TavallStudios/tavall-cloud/issues/495', 'github/TavallStudios/tavall-cloud/issues/460',
    'github/TavallStudios/tavall-cloud/issues/458', 'github/TavallStudios/tavall-cloud/issues/455', 'github/TavallStudios/tavall-cloud/issues/441', 'github/TavallStudios/tavall-cloud/issues/346',
    'github/TavallStudios/tavall-cloud/issues/280', 'github/TavallStudios/tavall-cloud/issues/241', 'github/TavallStudios/tavall-cloud/issues/191', 'github/TavallStudios/tavall-cli/issues/11',
    'github/TavallStudios/tavall-cli/issues/9', 'github/TavallStudios/tavall-cloud-networking/issues/4', 'venture/tavall-ai/venture-card', 'github/TavallStudios/tavall-ai/issues/120',
    'github/TavallStudios/tavall-ai/issues/119', 'github/TavallStudios/tavall-ai/issues/117', 'github/TavallStudios/tavall-ai/issues/114', 'github/TavallStudios/tavall-ai/issues/113',
    'github/TavallStudios/tavall-ai/issues/111', 'github/TavallStudios/tavall-ai/issues/110', 'github/TavallStudios/tavall-ai/issues/108', 'github/TavallStudios/tavall-ai/issues/107',
    'github/TavallStudios/tavall-ai/issues/106', 'github/TavallStudios/tavall-ai/issues/104', 'github/TavallStudios/tavall-ai/issues/103', 'github/TavallStudios/tavall-ai/issues/102',
    'github/TavallStudios/tavall-ai/issues/101', 'github/TavallStudios/tavall-ai/issues/89', 'github/TavallStudios/tavall-ai-memory/issues/4', 'venture/tavall-ai-chat/venture-card',
    'github/TavallStudios/tavall-ai-chat/issues/5', 'github/TavallStudios/tavall-ai-chat/issues/2', 'github/TavallStudios/tavall-web-ai-chat/issues/4', 'github/TavallStudios/tavall-web-ai-chat/issues/3',
    'github/TavallStudios/tavall-web-ai-chat/issues/2', 'venture/tavall-ai-plugin/venture-card', 'venture/tavall-mcp/venture-card', 'github/TavallStudios/tavall-web-mcp/issues/11',
    'github/TavallStudios/tavall-web-mcp/issues/10', 'venture/tavall-analytics/venture-card', 'github/TavallStudios/tavall-analytics/issues/22', 'github/TavallStudios/tavall-analytics/issues/21',
    'github/TavallStudios/tavall-analytics/issues/19', 'github/TavallStudios/tavall-analytics/issues/18', 'venture/tavall-workflows/venture-card', 'github/TavallStudios/tavall-workflows/issues/9',
    'github/TavallStudios/tavall-workflows/issues/8', 'github/TavallStudios/tavall-web-workflows/issues/7', 'venture/tavall-contractors/venture-card', 'github/TavallStudios/TavallContractors/issues/10',
    'github/TavallStudios/TavallContractors/issues/9', 'github/TavallStudios/tavall-web-contractors/issues/7', 'venture/tavall-couriers/venture-card', 'venture/tavall-novus/venture-card',
    'github/TavallStudios/tavall-mc/issues/401', 'github/TavallStudios/tavall-mc/issues/399', 'github/TavallStudios/tavall-mc/issues/398', 'github/TavallStudios/tavall-mc/issues/397',
    'github/TavallStudios/tavall-mc/issues/394', 'github/TavallStudios/tavall-mc/issues/393', 'github/TavallStudios/tavall-mc/issues/392', 'github/TavallStudios/tavall-mc/issues/386',
    'github/TavallStudios/tavall-mc/issues/360', 'github/TavallStudios/tavall-mc/issues/347', 'github/TavallStudios/tavall-mc/issues/306', 'github/TavallStudios/tavall-mc/issues/293',
    'github/TavallStudios/tavall-mc/issues/263', 'github/TavallStudios/tavall-mc/issues/255', 'github/TavallStudios/tavall-mc/issues/201', 'github/TavallStudios/tavall-web-mc/issues/11',
    'github/TavallStudios/tavall-minecraft-framework/issues/19', 'github/TavallStudios/tavall-minecraft-framework/issues/10', 'github/TavallStudios/tavall-mc-anticheat/issues/4', 'github/TavallStudios/tavall-mc-packet-filter/issues/4',
    'github/TavallStudios/tavall-mc-bot-testing/issues/1', 'venture/tavall-pvp/venture-card', 'venture/tavall-builder/venture-card', 'venture/tavall-affiliates/venture-card',
    'github/TavallStudios/tavall-affiliates/issues/4', 'github/TavallStudios/tavall-affiliates/issues/2', 'github/TavallStudios/tavall-web-affiliates/issues/2', 'github/TavallStudios/tavall-affiliate-marketing/issues/4',
    'github/TavallStudios/tavall-affiliate-marketing/issues/2', 'venture/tavall-labs/venture-card', 'github/TavallStudios/tavall-biz/issues/1', 'venture/tavall-ops/venture-card',
    'github/TavallStudios/tavall-web-ops/issues/19', 'github/TavallStudios/tavall-web-ops/issues/11', 'github/TavallStudios/tavall-web-ops/issues/8', 'github/TavallStudios/tavall-web-ops/issues/6',
    'venture/tavall-mc-speedrun/venture-card', 'venture/tavall-webstore/venture-card', 'venture/tavall-hytale/venture-card', 'venture/tavall-roblox/venture-card',
    'venture/tavall-open-harness/venture-card', 'github/TavallStudios/tavall-open-harness/issues/9', 'github/TavallStudios/tavall-open-harness/issues/8', 'github/TavallStudios/tavall-open-harness/issues/1',
    'github/TavallStudios/tavall-ci/issues/60', 'github/TavallStudios/tavall-ci/issues/57', 'github/TavallStudios/tavall-ci/issues/51', 'github/TavallStudios/tavall-ci/issues/50',
    'github/TavallStudios/tavall-ci/issues/24'
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM task_board_task
        WHERE project_key IN ('tavall-web', 'tavall-times-blog', 'tavall-account', 'tavall-dashboard', 'tavall-commerce', 'tavall-docs', 'tavall-qa', 'tavall-content', 'tavall-cloud', 'tavall-ai', 'tavall-ai-chat', 'tavall-ai-plugin', 'tavall-mcp', 'tavall-analytics', 'tavall-workflows', 'tavall-contractors', 'tavall-couriers', 'tavall-builder', 'tavall-affiliates', 'tavall-labs', 'tavall-ops', 'tavall-mc-speedrun', 'tavall-webstore', 'tavall-hytale', 'tavall-roblox', 'tavall-open-harness')
    ) THEN
        RAISE EXCEPTION 'Migration 042 found tasks still attached to a venture project; refusing project deletion';
    END IF;
END $$;

DELETE FROM task_board_project
WHERE project_key IN ('tavall-web', 'tavall-times-blog', 'tavall-account', 'tavall-dashboard', 'tavall-commerce', 'tavall-docs', 'tavall-qa', 'tavall-content', 'tavall-cloud', 'tavall-ai', 'tavall-ai-chat', 'tavall-ai-plugin', 'tavall-mcp', 'tavall-analytics', 'tavall-workflows', 'tavall-contractors', 'tavall-couriers', 'tavall-builder', 'tavall-affiliates', 'tavall-labs', 'tavall-ops', 'tavall-mc-speedrun', 'tavall-webstore', 'tavall-hytale', 'tavall-roblox', 'tavall-open-harness');
