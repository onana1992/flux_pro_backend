-- Objectif : introduire le référentiel organization_type en BDD et migrer organization.type (enum string) → organization_type_id
-- Tables impactées : organization_type (création), organization (ALTER)
-- Prérequis : script V1__create_users_organizations_auth.sql déjà exécuté
-- Exécution : manuelle sur MySQL avant déploiement du code v1.1 (ddl-auto=none)

-- ── 1. Création table types ──
CREATE TABLE IF NOT EXISTS organization_type (
    id                CHAR(36)     NOT NULL PRIMARY KEY,
    code              VARCHAR(32)  NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    name_en           VARCHAR(255) NULL,
    description       TEXT         NULL,
    color             VARCHAR(20)  NULL,
    sort_order        INT          NOT NULL DEFAULT 0,
    allows_root       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_regional_scope BOOLEAN      NOT NULL DEFAULT FALSE,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_organization_type_code ON organization_type(code);

-- ── 2. Seed types MINTP (UUIDs fixes pour reproductibilité) ──
INSERT INTO organization_type (id, code, name, name_en, color, sort_order, allows_root, is_regional_scope, active)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'MINISTRY',              'Ministère',  'Ministry',              'purple', 1, TRUE,  FALSE, TRUE),
    ('00000000-0000-4000-8000-000000000002', 'DIRECTORATE',           'Direction',  'Directorate',           'blue',   2, FALSE, FALSE, TRUE),
    ('00000000-0000-4000-8000-000000000003', 'DIVISION',              'Division',   'Division',              'gray',   3, FALSE, FALSE, TRUE),
    ('00000000-0000-4000-8000-000000000004', 'SERVICE',               'Service',    'Service',               'green',  4, FALSE, FALSE, TRUE),
    ('00000000-0000-4000-8000-000000000005', 'REGIONAL_DIRECTORATE',  'DRTP',       'Regional directorate',  'orange', 5, FALSE, TRUE,  TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    name_en = VALUES(name_en),
    color = VALUES(color),
    sort_order = VALUES(sort_order),
    allows_root = VALUES(allows_root),
    is_regional_scope = VALUES(is_regional_scope),
    active = VALUES(active);

-- ── 3. Ajout FK sur organization ──
ALTER TABLE organization
    ADD COLUMN organization_type_id CHAR(36) NULL AFTER name;

-- ── 4. Migration des valeurs existantes (colonne type enum string) ──
UPDATE organization o
    INNER JOIN organization_type ot ON ot.code = o.type
SET o.organization_type_id = ot.id
WHERE o.organization_type_id IS NULL;

-- Vérifier les lignes orphelines avant de continuer :
-- SELECT id, code, type FROM organization WHERE organization_type_id IS NULL;

ALTER TABLE organization
    MODIFY organization_type_id CHAR(36) NOT NULL,
    ADD CONSTRAINT fk_organization_type
        FOREIGN KEY (organization_type_id) REFERENCES organization_type(id);

CREATE INDEX IF NOT EXISTS idx_organization_type_id ON organization(organization_type_id);

-- ── 5. Suppression ancienne colonne enum ──
ALTER TABLE organization DROP COLUMN type;

-- rollback (commenté) :
-- ALTER TABLE organization ADD COLUMN type VARCHAR(30) NULL;
-- UPDATE organization o INNER JOIN organization_type ot ON ot.id = o.organization_type_id SET o.type = ot.code;
-- ALTER TABLE organization DROP FOREIGN KEY fk_organization_type;
-- ALTER TABLE organization DROP COLUMN organization_type_id;
-- DROP TABLE organization_type;
