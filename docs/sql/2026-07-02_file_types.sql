-- Objectif : référentiel des types de dossiers MINTP (catalogue DOS-03 / DOS-06)
-- Tables impactées : file_types (création)
-- Prérequis : UUID en BINARY(16) — aligné Hibernate BaseEntity
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
-- Seed T01–T05 : appliqué au démarrage par FileTypeDataInitializer

CREATE TABLE IF NOT EXISTS file_types (
    id                          BINARY(16)   NOT NULL PRIMARY KEY,
    code                        VARCHAR(32)  NOT NULL UNIQUE,
    name                        VARCHAR(255) NOT NULL,
    name_en                     VARCHAR(255) NULL,
    description                 TEXT         NULL,
    direction_code              VARCHAR(32)  NULL,
    sort_order                  INT          NOT NULL DEFAULT 0,
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_file_types_active (active),
    INDEX idx_file_types_direction (direction_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS file_types;
