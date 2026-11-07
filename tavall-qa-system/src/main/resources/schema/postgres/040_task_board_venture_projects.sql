-- W14-hy3-ventures (2026-11-07): seed Tavall venture board projects.
-- The QA board models ventures as task_board_project rows. This migration populates one
-- project per Tavall venture with its purpose/data so the board (and owner QA review) can
-- surface every venture. Existing venture projects (project-novus, tavall-pvp) are updated
-- with current data rather than duplicated. Audience PUBLIC for user-facing products,
-- ADMIN_ONLY for internal infrastructure ventures.
INSERT INTO task_board_project (project_key, display_name, description, project_status, audience, sort_order, created_at, updated_at) VALUES
    ('tavall-web', 'Tavall Web', 'Core Tavall Web runtime: service router, company home (tavall.org), Deals and Releases surfaces. Live (PROD BLUE).', 'ACTIVE', 'PUBLIC', 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-times-blog', 'Tavall Times Blog', 'Tavall Times editorial blog publishing Deals and Releases. Embedded in tavall-web PROD BLUE (blog.tavall.org).', 'ACTIVE', 'PUBLIC', 210, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-account', 'Tavall Account', 'Native identity authority: OAuth2/OIDC auth server (Google, Discord, MS). Underpins all monetization SSO.', 'ACTIVE', 'PUBLIC', 220, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-dashboard', 'Tavall Dashboard', 'Creator and user dashboard. Unauthenticated visitors redirect to account.tavall.org.', 'ACTIVE', 'PUBLIC', 230, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-commerce', 'Tavall Commerce', 'Commerce, store, settlement, entitlement, creator ledger and payout preview (P0 #76). commerce.tavall.org.', 'ACTIVE', 'PUBLIC', 240, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-docs', 'Tavall Docs', 'Public documentation viewer and Markdown documentation authority (docs.tavall.org, tavall-docs repo).', 'ACTIVE', 'PUBLIC', 250, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-qa', 'Tavall QA Board', 'QA Task Board authority and public execution view (qa.tavall.org). This worker models venture data here.', 'ACTIVE', 'PUBLIC', 260, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-content', 'Tavall Content', 'Content studio backend and web clipping surface (P0 #59/#76). Backend PROD BLUE (content.tavall.org).', 'ACTIVE', 'PUBLIC', 270, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-cloud', 'Tavall Cloud', 'MCP-first control plane: agent, data, storage, cluster, backup, command (tavall-cloud, tavall-cli).', 'ACTIVE', 'ADMIN_ONLY', 280, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-ai', 'Tavall AI', 'AI platform: agents, prompt memory, model tooling (tavall-ai, tavall-ai-memory).', 'ACTIVE', 'PUBLIC', 290, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-ai-chat', 'Tavall AI Chat', 'AI chat user surface (tavall-ai-chat, tavall-web-ai-chat). ai.tavall.org.', 'ACTIVE', 'PUBLIC', 300, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-ai-plugin', 'Tavall AI Plugin', 'Tavall AI plugin / agent harness (this runtime). Internal tooling; deploy prohibited by policy.', 'ACTIVE', 'ADMIN_ONLY', 310, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-mcp', 'Tavall MCP', 'Web MCP transport (tavall-web-mcp, mcp.tavall.org).', 'ACTIVE', 'PUBLIC', 320, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-analytics', 'Tavall Analytics', 'Product and user funnel analytics (tavall-analytics, analytics.tavall.org).', 'ACTIVE', 'PUBLIC', 330, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-workflows', 'Tavall Workflows', 'Workflow automation surface (tavall-workflows, tavall-web-workflows, workflows.tavall.org).', 'ACTIVE', 'PUBLIC', 340, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-contractors', 'Tavall Contractors', 'Contractors and talent platform (TavallContractors, tavall-web-contractors). Marketplace (pre-revenue).', 'ACTIVE', 'PUBLIC', 350, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-couriers', 'Tavall Couriers', 'Stage: building. Couriers platform (Spring webview, TavallCouriers). Logistics (pre-revenue).', 'ACTIVE', 'ADMIN_ONLY', 360, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-builder', 'Tavall Builder', 'Stage: building/idea. Builder Studio internal site/content builder tool (referenced as BUILDER_STUDIO action).', 'ACTIVE', 'ADMIN_ONLY', 370, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-affiliates', 'Tavall Affiliates', 'Stage: building. Affiliate marketing (tavall-affiliates, tavall-web-affiliates, tavall-affiliate-marketing).', 'ACTIVE', 'ADMIN_ONLY', 380, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-labs', 'Tavall Labs', 'Stage: idea/building. Labs / Biz experiments (tavall-labs, tavall-web-labs, tavall-biz, tavall-orgs).', 'ACTIVE', 'ADMIN_ONLY', 390, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-ops', 'Tavall Ops', 'Private operations control plane (tavall-web-ops). Tailscale/Nebula overlay only.', 'ACTIVE', 'ADMIN_ONLY', 400, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-mc-speedrun', 'Tavall MC Speedrun', 'Stage: building. Minecraft speedrun platform (MCRSpeedrun). Game/store (pre-revenue).', 'ACTIVE', 'ADMIN_ONLY', 410, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-webstore', 'Tavall Webstore', 'Stage: building. Multi-tenant commerce platform (Webstore, tavall-web-store-tools). Overlaps tavall-commerce.', 'ACTIVE', 'ADMIN_ONLY', 420, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-hytale', 'Tavall Hytale', 'Stage: building/idea. Hytale ventures: HyRhythm, resource-game vertical slice, bot harness.', 'ACTIVE', 'ADMIN_ONLY', 430, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-roblox', 'Tavall Roblox', 'Stage: idea. Roblox presence (tavall-roblox). Pre-revenue.', 'ACTIVE', 'ADMIN_ONLY', 440, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-open-harness', 'Tavall Open Harness', 'CI / open harness (tavall-open-harness, tavall-ci). Internal CI gate authority.', 'ACTIVE', 'ADMIN_ONLY', 450, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (project_key) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    description = EXCLUDED.description,
    project_status = EXCLUDED.project_status,
    audience = EXCLUDED.audience,
    sort_order = EXCLUDED.sort_order,
    updated_at = CURRENT_TIMESTAMP;
