-- Objectif : catalogue administrable des types d'alerte (ALR-F17) — PAS une énumération figée
-- Tables impactées : alert_types (création)
-- Prérequis : UUID en BINARY(16) — aligné Hibernate BaseEntity
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
-- Seed REMINDER/OVERDUE/ESCALATION/DAILY_DIGEST : appliqué au démarrage par AlertTypeDataInitializer

CREATE TABLE IF NOT EXISTS alert_types (
    id                   BINARY(16)   NOT NULL PRIMARY KEY,
    code                 VARCHAR(30)  NOT NULL UNIQUE,
    label                VARCHAR(100) NOT NULL,
    description          TEXT         NULL,
    email_template_code  VARCHAR(100) NULL,
    system_defined       BOOLEAN      NOT NULL DEFAULT FALSE,
    active               BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_alert_types_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS alert_types;
