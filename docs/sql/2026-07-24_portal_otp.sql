-- Objectif : OTP portail externe (P3)
-- Tables impactées : portal_otp_challenges (création)
-- Prérequis : portal_users (2026-07-24_portal_foundations.sql)
-- Exécution : manuelle sur MySQL (ddl-auto=none)

CREATE TABLE IF NOT EXISTS portal_otp_challenges (
    id              BINARY(16)   NOT NULL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    purpose         VARCHAR(32)  NOT NULL,
    code_hash       VARCHAR(255) NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    consumed_at     DATETIME(6)  NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_portal_otp_email_purpose (email, purpose),
    INDEX idx_portal_otp_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS portal_otp_challenges;
