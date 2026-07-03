-- Objectif : tables templates de chaîne (CHN-TPL) — chain_templates, chain_step_templates
-- Tables impactées : chain_templates (création), chain_step_templates (création)
-- Prérequis : schéma users existant ; UUID en BINARY(16)
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
-- Seed T01–T05 : appliqué au démarrage par ChainDataInitializer (ou ré-exécuter après vidage)

CREATE TABLE IF NOT EXISTS chain_templates (
    id               BINARY(16)   NOT NULL PRIMARY KEY,
    code             VARCHAR(10)  NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT         NULL,
    file_type_code   VARCHAR(32)  NULL,
    total_delay_days INT          NOT NULL DEFAULT 0,
    delay_unit       VARCHAR(20)  NOT NULL DEFAULT 'WORKING_DAYS',
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    system_template  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_chain_templates_active (active),
    INDEX idx_chain_templates_file_type (file_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chain_step_templates (
    id                BINARY(16)   NOT NULL PRIMARY KEY,
    chain_template_id BINARY(16)   NOT NULL,
    step_order        INT          NOT NULL,
    label             VARCHAR(255) NOT NULL,
    responsible_role  VARCHAR(30)  NOT NULL,
    delay_value       INT          NOT NULL DEFAULT 0,
    delay_unit        VARCHAR(20)  NOT NULL DEFAULT 'WORKING_DAYS',
    expected_action   VARCHAR(500) NULL,
    optional          BOOLEAN      NOT NULL DEFAULT FALSE,
    closure_step      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_step_template_chain
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id) ON DELETE CASCADE,
    UNIQUE KEY uk_chain_step_order (chain_template_id, step_order),
    INDEX idx_step_template_chain (chain_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS chain_step_templates;
-- DROP TABLE IF EXISTS chain_templates;
