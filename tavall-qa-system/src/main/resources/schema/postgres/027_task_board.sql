CREATE TABLE IF NOT EXISTS task_board_project (
    project_key VARCHAR(96) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    project_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    audience VARCHAR(32) NOT NULL DEFAULT 'PUBLIC',
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_project_status_check CHECK (project_status IN ('ACTIVE', 'PAUSED', 'SHIPPED', 'ARCHIVED')),
    CONSTRAINT task_board_project_audience_check CHECK (audience IN ('PUBLIC', 'QA_TESTER', 'STAFF', 'ADMIN_ONLY'))
);

CREATE TABLE IF NOT EXISTS task_board_task (
    task_id VARCHAR(36) PRIMARY KEY,
    project_key VARCHAR(96) NOT NULL REFERENCES task_board_project(project_key) ON DELETE CASCADE,
    section_key VARCHAR(128) NOT NULL DEFAULT 'general',
    title VARCHAR(220) NOT NULL,
    public_title VARCHAR(220) NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    public_summary TEXT NOT NULL DEFAULT '',
    task_status VARCHAR(32) NOT NULL DEFAULT 'BACKLOG',
    priority VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    qa_status VARCHAR(32) NOT NULL DEFAULT 'NHV',
    audience VARCHAR(32) NOT NULL DEFAULT 'ADMIN_ONLY',
    weight INTEGER NOT NULL DEFAULT 1,
    host_validation_required BOOLEAN NOT NULL DEFAULT FALSE,
    source_type VARCHAR(64) NOT NULL DEFAULT '',
    source_reference VARCHAR(1024) NOT NULL DEFAULT '',
    created_by_minecraft_uuid VARCHAR(36),
    verified_by_minecraft_uuid VARCHAR(36),
    human_verified_at TIMESTAMPTZ,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_task_status_check CHECK (task_status IN ('BACKLOG', 'READY', 'ACTIVE', 'BLOCKED', 'REVIEW', 'DONE', 'CANCELLED')),
    CONSTRAINT task_board_task_priority_check CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT task_board_task_qa_check CHECK (qa_status IN ('NHV', 'REVIEW_REQUIRED', 'HUMAN_VERIFIED')),
    CONSTRAINT task_board_task_audience_check CHECK (audience IN ('PUBLIC', 'QA_TESTER', 'STAFF', 'ADMIN_ONLY')),
    CONSTRAINT task_board_task_weight_check CHECK (weight >= 1),
    CONSTRAINT task_board_task_human_verification_check CHECK (
        (qa_status = 'HUMAN_VERIFIED' AND verified_by_minecraft_uuid IS NOT NULL AND human_verified_at IS NOT NULL)
        OR
        (qa_status <> 'HUMAN_VERIFIED' AND verified_by_minecraft_uuid IS NULL AND human_verified_at IS NULL)
    )
);

CREATE TABLE IF NOT EXISTS task_board_action (
    action_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    action_type VARCHAR(32) NOT NULL,
    label VARCHAR(160) NOT NULL,
    target VARCHAR(2048) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_action_type_check CHECK (action_type IN ('ADMIN_ROUTE', 'REPOSITORY_FILE', 'BUILDER_STUDIO', 'DISCORD_THREAD', 'EXTERNAL_LINK'))
);

CREATE TABLE IF NOT EXISTS task_board_comment (
    comment_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    parent_comment_id VARCHAR(36) REFERENCES task_board_comment(comment_id) ON DELETE CASCADE,
    author_minecraft_uuid VARCHAR(36),
    comment_type VARCHAR(32) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT task_board_comment_type_check CHECK (comment_type IN ('COMMENT', 'SUGGESTION', 'QA_NOTE'))
);

CREATE TABLE IF NOT EXISTS qa_vqc_contribution (
    contribution_id VARCHAR(36) PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL REFERENCES task_board_task(task_id) ON DELETE CASCADE,
    contributor_minecraft_uuid VARCHAR(36) NOT NULL,
    base_vqc INTEGER NOT NULL DEFAULT 1,
    bonus_vqc INTEGER NOT NULL DEFAULT 0,
    bonus_approved BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NOT NULL DEFAULT '',
    awarded_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_task_board_project_order ON task_board_project(sort_order, display_name);
CREATE INDEX IF NOT EXISTS idx_task_board_task_board ON task_board_task(project_key, section_key, task_status, sort_order);
CREATE INDEX IF NOT EXISTS idx_task_board_task_public ON task_board_task(project_key, audience, section_key, sort_order);
CREATE INDEX IF NOT EXISTS idx_task_board_task_qa ON task_board_task(project_key, qa_status, host_validation_required);
CREATE INDEX IF NOT EXISTS idx_task_board_action_task ON task_board_action(task_id, sort_order);
CREATE INDEX IF NOT EXISTS idx_task_board_comment_task ON task_board_comment(task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_qa_vqc_contributor ON qa_vqc_contribution(contributor_minecraft_uuid, awarded_at);

INSERT INTO task_board_project (project_key, display_name, description, project_status, audience, sort_order, created_at, updated_at)
VALUES
    ('project-novus', 'Project Novus', 'Public development, gameplay, QA, and shipping progress for Project Novus.', 'ACTIVE', 'PUBLIC', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('tavall-pvp', 'Tavall PvP', 'Public gameplay, QA, and release progress for Tavall PvP.', 'ACTIVE', 'PUBLIC', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (project_key) DO NOTHING;
