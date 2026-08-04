-- Objectif : Sprint 0 assistant IA — permission ASSISTANT:USE + tables de conversation
-- Tables impactées : permissions, role_permissions, assistant_conversation, assistant_message, assistant_tool_call
-- Prérequis : docs/sql/2026-07-02_rbac_roles_permissions.sql (rôles / permissions de base)
-- Cible : PostgreSQL (Neon) — ddl-auto=none
-- Exécution : manuelle avant déploiement du code assistant

-- ---------------------------------------------------------------------------
-- 1. Permission ASSISTANT:USE (tous les rôles métier actifs)
-- ---------------------------------------------------------------------------
INSERT INTO permissions (id, name, resource, action, description, created_at, updated_at)
SELECT gen_random_uuid(), v.name, v.resource, v.action, v.name, NOW(), NOW()
FROM (
    SELECT 'ASSISTANT:USE' AS name, 'ASSISTANT' AS resource, 'USE' AS action
) v
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.name = v.name);

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name = 'ASSISTANT:USE'
WHERE r.name IN (
    'SUPER_ADMIN', 'BUSINESS_ADMIN', 'DIRECTOR', 'SERVICE_HEAD', 'REGIONAL_DIRECTOR',
    'SECRETARY_GENERAL', 'EXECUTIVE_OFFICE', 'AGENT', 'SUPPORT', 'READER'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
);

-- ---------------------------------------------------------------------------
-- 2. Tables conversation
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS assistant_conversation (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(200) NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_asst_conv_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_asst_conv_user_updated
    ON assistant_conversation (user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS assistant_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    refused BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_asst_msg_conv FOREIGN KEY (conversation_id)
        REFERENCES assistant_conversation (id) ON DELETE CASCADE,
    CONSTRAINT chk_asst_msg_role CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX IF NOT EXISTS idx_asst_msg_conv_created
    ON assistant_message (conversation_id, created_at);

CREATE TABLE IF NOT EXISTS assistant_tool_call (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL,
    tool_name VARCHAR(80) NOT NULL,
    arguments_json TEXT NULL,
    result_summary VARCHAR(500) NULL,
    success BOOLEAN NOT NULL,
    duration_ms INT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_asst_tool_msg FOREIGN KEY (message_id)
        REFERENCES assistant_message (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_asst_tool_message
    ON assistant_tool_call (message_id);

-- rollback (commenté) :
-- DROP TABLE IF EXISTS assistant_tool_call;
-- DROP TABLE IF EXISTS assistant_message;
-- DROP TABLE IF EXISTS assistant_conversation;
-- DELETE FROM role_permissions WHERE permission_id IN (
--     SELECT id FROM permissions WHERE resource = 'ASSISTANT');
-- DELETE FROM permissions WHERE resource = 'ASSISTANT';
