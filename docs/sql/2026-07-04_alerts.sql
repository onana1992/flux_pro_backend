-- Objectif : historique des alertes générées/envoyées + notifications in-app (ALR-05)
-- Tables impactées : alerts (création)
-- Prérequis : files, file_passages, alert_rules, alert_types, users
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
--
-- file_id / file_passage_id / alert_rule_id sont NULL-ables : le digest quotidien (ALR-08)
-- agrège plusieurs dossiers et n'est donc rattaché ni à un dossier ni à un maillon précis.

CREATE TABLE IF NOT EXISTS alerts (
    id                  BINARY(16)   NOT NULL PRIMARY KEY,
    file_id             BINARY(16)   NULL,
    file_passage_id     BINARY(16)   NULL,
    alert_rule_id       BINARY(16)   NULL,
    alert_type_id       BINARY(16)   NOT NULL,
    escalation_level    INT          NULL,
    channel             VARCHAR(10)  NOT NULL,
    recipient_user_id   BINARY(16)   NOT NULL,
    status              VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    sent_at             DATETIME(6)  NULL,
    read_at             DATETIME(6)  NULL,
    error_message       VARCHAR(500) NULL,
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alert_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_file_passage
        FOREIGN KEY (file_passage_id) REFERENCES file_passages(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_rule
        FOREIGN KEY (alert_rule_id) REFERENCES alert_rules(id) ON DELETE SET NULL,
    CONSTRAINT fk_alert_type
        FOREIGN KEY (alert_type_id) REFERENCES alert_types(id),
    CONSTRAINT fk_alert_recipient
        FOREIGN KEY (recipient_user_id) REFERENCES users(id),
    UNIQUE KEY uk_alert_idempotence (file_passage_id, alert_rule_id, channel),
    INDEX idx_alerts_file (file_id),
    INDEX idx_alerts_recipient (recipient_user_id),
    INDEX idx_alerts_recipient_channel_read (recipient_user_id, channel, read_at),
    INDEX idx_alerts_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS alerts;
