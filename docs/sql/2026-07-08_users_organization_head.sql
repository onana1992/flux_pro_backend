-- Objectif : marquer le chef d'une unité organisationnelle (un seul par organisation)
-- Table impactée : users
-- Prérequis : schéma users existant (V1 ou équivalent)

ALTER TABLE users
    ADD COLUMN organization_head BOOLEAN NOT NULL DEFAULT FALSE
    AFTER active;

CREATE INDEX idx_users_org_head ON users (organization_id, organization_head);

-- rollback (commenté) :
-- DROP INDEX idx_users_org_head ON users;
-- ALTER TABLE users DROP COLUMN organization_head;
