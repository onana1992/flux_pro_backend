-- Objectif : introduire les dossiers préconfigurés (formulaire + circuit)
--            et détacher la config portail de file_types
-- Tables impactées : preconfigured_dossiers (CREATE) ; files (ALTER) ; file_types (DROP colonnes portal_*)
-- Prérequis : file_types, chain_templates, files, docs/sql/2026-07-24_portal_foundations.sql
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)

-- ---------------------------------------------------------------------------
-- 1. Table dossiers préconfigurés
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS preconfigured_dossiers (
    id                          BINARY(16)   NOT NULL PRIMARY KEY,
    code                        VARCHAR(32)  NOT NULL,
    name                        VARCHAR(255) NOT NULL,
    name_en                     VARCHAR(255) NULL,
    description                 TEXT         NULL,
    file_type_code              VARCHAR(32)  NOT NULL,
    chain_template_id           BINARY(16)   NULL,
    step_assignments            LONGTEXT     NULL,
    default_first_step_responsible_user_id BINARY(16) NULL,
    direction_code              VARCHAR(32)  NULL,
    sort_order                  INT          NOT NULL DEFAULT 0,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    portal_enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
    portal_audience             VARCHAR(20)  NULL,
    form_schema                 LONGTEXT     NULL,
    required_attachment_keys    LONGTEXT     NULL,
    created_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_preconfigured_dossiers_code UNIQUE (code),
    CONSTRAINT fk_preconfigured_dossiers_chain
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id),
    CONSTRAINT fk_preconfigured_dossiers_responsible
        FOREIGN KEY (default_first_step_responsible_user_id) REFERENCES users(id),
    INDEX idx_preconfigured_dossiers_active (active),
    INDEX idx_preconfigured_dossiers_portal (portal_enabled),
    INDEX idx_preconfigured_dossiers_file_type (file_type_code),
    INDEX idx_preconfigured_dossiers_responsible (default_first_step_responsible_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 2. Lien instance dossier → préconfiguré
-- ---------------------------------------------------------------------------
ALTER TABLE files
    ADD COLUMN preconfigured_dossier_id BINARY(16) NULL AFTER file_type_code;

ALTER TABLE files
    ADD CONSTRAINT fk_files_preconfigured_dossier
        FOREIGN KEY (preconfigured_dossier_id) REFERENCES preconfigured_dossiers(id);

CREATE INDEX idx_files_preconfigured_dossier ON files (preconfigured_dossier_id);

-- ---------------------------------------------------------------------------
-- 3. Migration : file_types portal_* → preconfigured_dossiers
-- ---------------------------------------------------------------------------
INSERT INTO preconfigured_dossiers (
    id, code, name, name_en, description,
    file_type_code, chain_template_id, direction_code, sort_order,
    active, portal_enabled, portal_audience, form_schema, required_attachment_keys,
    created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    ft.code,
    ft.name,
    ft.name_en,
    ft.description,
    ft.code,
    (
        SELECT ct.id
        FROM chain_templates ct
        WHERE ct.file_type_code = ft.code AND ct.active = TRUE
        ORDER BY ct.created_at ASC
        LIMIT 1
    ),
    ft.direction_code,
    ft.sort_order,
    ft.active,
    ft.portal_enabled,
    ft.portal_audience,
    ft.portal_form_schema,
    ft.portal_required_attachment_keys,
    NOW(6),
    NOW(6)
FROM file_types ft
WHERE ft.portal_enabled = TRUE
  AND NOT EXISTS (
      SELECT 1 FROM preconfigured_dossiers pd WHERE pd.code = ft.code
  );

-- ---------------------------------------------------------------------------
-- 4. Retirer la config portail de file_types (classification seule)
-- ---------------------------------------------------------------------------
DROP INDEX idx_file_types_portal_enabled ON file_types;

ALTER TABLE file_types
    DROP COLUMN portal_required_attachment_keys,
    DROP COLUMN portal_form_schema,
    DROP COLUMN portal_audience,
    DROP COLUMN portal_enabled;

-- rollback (commenté) :
-- ALTER TABLE file_types
--     ADD COLUMN portal_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER active,
--     ADD COLUMN portal_audience VARCHAR(20) NULL AFTER portal_enabled,
--     ADD COLUMN portal_form_schema LONGTEXT NULL AFTER portal_audience,
--     ADD COLUMN portal_required_attachment_keys LONGTEXT NULL AFTER portal_form_schema;
-- CREATE INDEX idx_file_types_portal_enabled ON file_types (portal_enabled);
-- ALTER TABLE files DROP FOREIGN KEY fk_files_preconfigured_dossier;
-- DROP INDEX idx_files_preconfigured_dossier ON files;
-- ALTER TABLE files DROP COLUMN preconfigured_dossier_id;
-- DROP TABLE IF EXISTS preconfigured_dossiers;
