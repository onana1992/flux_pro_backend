-- Objectif : aligner business_calendar sur BaseEntity (updated_at) + seed fériés CM 2026-2027
-- Tables impactées : business_calendar
-- Exécution : manuelle sur MySQL (ddl-auto=none)

ALTER TABLE business_calendar
    ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NULL;

UPDATE business_calendar
SET updated_at = COALESCE(created_at, CURRENT_TIMESTAMP(6))
WHERE updated_at IS NULL;

ALTER TABLE business_calendar
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

INSERT IGNORE INTO business_calendar (id, calendar_date, label, country_code, created_at, updated_at)
VALUES
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-01-01', 'Nouvel An', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-02-11', 'Fête de la Jeunesse', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-05-01', 'Fête du Travail', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-05-20', 'Fête Nationale', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2026-12-25', 'Noël', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2027-01-01', 'Nouvel An', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2027-02-11', 'Fête de la Jeunesse', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2027-05-01', 'Fête du Travail', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2027-05-20', 'Fête Nationale', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (UNHEX(REPLACE(UUID(), '-', '')), '2027-12-25', 'Noël', 'CM', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
