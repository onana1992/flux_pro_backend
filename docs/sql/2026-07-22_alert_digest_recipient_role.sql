-- Objectif : rôles destinataires du digest quotidien (ALR-08), configurables via UI
-- Tables impactées : alert_digest_recipient_role (création)
-- Exécution : manuelle sur MySQL (ddl-auto=none)
-- Seed DIRECTOR : aussi appliqué au démarrage si table vide (AlertDigestRecipientRoleDataInitializer)

CREATE TABLE IF NOT EXISTS alert_digest_recipient_role (
    id           BINARY(16)   NOT NULL PRIMARY KEY,
    role         VARCHAR(40)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_digest_recipient_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO alert_digest_recipient_role (id, role, created_at, updated_at)
VALUES (UNHEX(REPLACE(UUID(), '-', '')), 'DIRECTOR', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
