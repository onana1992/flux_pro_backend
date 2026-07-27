-- Objectif : fondations PORTAL (P1) — utilisateurs portail, assouplir FKs dossier/PJ
-- NOTE 2026-07-24 : les colonnes portal_* sur file_types (section 2) sont SUPERSEDÉES
--   par docs/sql/2026-07-24_preconfigured_dossiers.sql (entité PreconfiguredDossier).
--   Sur une base neuve : exécuter ce script puis immédiatement preconfigured_dossiers.sql
--   (qui migre / droppe les colonnes portal_* de file_types).
-- Tables impactées : portal_users (création) ; file_types, files, file_attachments (ALTER)
-- Prérequis : users, organization, chain_templates, file_types, files, file_attachments (UUID BINARY(16))
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)

-- ---------------------------------------------------------------------------
-- 1. Utilisateurs du portail (distincts de users métier)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS portal_users (
    id                          BINARY(16)   NOT NULL PRIMARY KEY,
    email                       VARCHAR(255) NOT NULL,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    phone                       VARCHAR(20)  NULL,
    portal_user_type            VARCHAR(32)  NOT NULL,
    staff_number                VARCHAR(32)  NULL,
    organization_id             BINARY(16)   NULL,
    password_hash               VARCHAR(255) NULL,
    must_change_password        BOOLEAN      NOT NULL DEFAULT FALSE,
    password_changed_at         DATETIME(6)  NULL,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    email_verified_at           DATETIME(6)  NULL,
    created_by_admin_user_id    BINARY(16)   NULL,
    created_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_portal_users_email UNIQUE (email),
    CONSTRAINT fk_portal_users_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_portal_users_created_by_admin
        FOREIGN KEY (created_by_admin_user_id) REFERENCES users(id),
    INDEX idx_portal_users_type (portal_user_type),
    INDEX idx_portal_users_active (active),
    INDEX idx_portal_users_staff (staff_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 2. FileType — ouverture portail + schéma + chaîne + audience
-- ---------------------------------------------------------------------------
ALTER TABLE file_types
    ADD COLUMN portal_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER active,
    ADD COLUMN portal_audience VARCHAR(20) NULL AFTER portal_enabled,
    ADD COLUMN portal_form_schema LONGTEXT NULL AFTER portal_audience,
    ADD COLUMN portal_required_attachment_keys LONGTEXT NULL AFTER portal_form_schema;

CREATE INDEX idx_file_types_portal_enabled ON file_types (portal_enabled);

-- ---------------------------------------------------------------------------
-- 3. Files — créateur métier nullable + lien PortalUser
-- ---------------------------------------------------------------------------
ALTER TABLE files
    MODIFY COLUMN created_by_user_id BINARY(16) NULL;

ALTER TABLE files
    ADD COLUMN portal_user_id BINARY(16) NULL AFTER created_by_user_id;

ALTER TABLE files
    ADD CONSTRAINT fk_files_portal_user
        FOREIGN KEY (portal_user_id) REFERENCES portal_users(id);

CREATE INDEX idx_files_portal_user ON files (portal_user_id);

-- ---------------------------------------------------------------------------
-- 4. File attachments — uploader métier nullable + uploader portail
-- ---------------------------------------------------------------------------
ALTER TABLE file_attachments
    MODIFY COLUMN uploaded_by_id BINARY(16) NULL;

ALTER TABLE file_attachments
    ADD COLUMN uploaded_by_portal_user_id BINARY(16) NULL AFTER uploaded_by_id;

ALTER TABLE file_attachments
    ADD CONSTRAINT fk_attachment_portal_user
        FOREIGN KEY (uploaded_by_portal_user_id) REFERENCES portal_users(id);

CREATE INDEX idx_attachments_portal_user ON file_attachments (uploaded_by_portal_user_id);

-- rollback (commenté) :
-- ALTER TABLE file_attachments DROP FOREIGN KEY fk_attachment_portal_user;
-- DROP INDEX idx_attachments_portal_user ON file_attachments;
-- ALTER TABLE file_attachments DROP COLUMN uploaded_by_portal_user_id;
-- ALTER TABLE file_attachments MODIFY COLUMN uploaded_by_id BINARY(16) NOT NULL;
-- ALTER TABLE files DROP FOREIGN KEY fk_files_portal_user;
-- DROP INDEX idx_files_portal_user ON files;
-- ALTER TABLE files DROP COLUMN portal_user_id;
-- ALTER TABLE files MODIFY COLUMN created_by_user_id BINARY(16) NOT NULL;
-- DROP INDEX idx_file_types_portal_enabled ON file_types;
-- ALTER TABLE file_types
--     DROP COLUMN portal_required_attachment_keys,
--     DROP COLUMN portal_form_schema,
--     DROP COLUMN portal_audience,
--     DROP COLUMN portal_enabled;
-- DROP TABLE IF EXISTS portal_users;
