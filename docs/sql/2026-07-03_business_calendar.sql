-- Objectif : calendrier ouvré — jours fériés Cameroun (module DEL / CHN)
-- Tables impactées : business_calendar (création)
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)

CREATE TABLE IF NOT EXISTS business_calendar (
    id           BINARY(16)  NOT NULL PRIMARY KEY,
    calendar_date DATE       NOT NULL,
    label        VARCHAR(100) NOT NULL,
    country_code VARCHAR(2)  NOT NULL DEFAULT 'CM',
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_calendar_date_country (calendar_date, country_code),
    INDEX idx_calendar_country (country_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed fériés Cameroun (dates récurrentes — année civile)
INSERT IGNORE INTO business_calendar (id, calendar_date, label, country_code, created_at, updated_at)
VALUES
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-01-01', 'Nouvel An', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-02-11', 'Fête de la Jeunesse', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-05-01', 'Fête du Travail', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-05-20', 'Fête Nationale', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-12-25', 'Noël', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- rollback (commenté) :
-- DROP TABLE IF EXISTS business_calendar;
