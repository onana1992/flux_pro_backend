-- Objectif : module CHN-PASS — instances de maillons sur dossiers
-- Tables impactées : file_passages (création)
-- Prérequis : files, chain_step_templates, users
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)

CREATE TABLE IF NOT EXISTS file_passages (
    id                     BINARY(16)     NOT NULL PRIMARY KEY,
    file_id                BINARY(16)     NOT NULL,
    chain_step_template_id BINARY(16)     NOT NULL,
    step_order             INT            NOT NULL,
    responsible_user_id    BINARY(16)     NULL,
    status                 VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    received_at            DATETIME(6)    NULL,
    transmitted_at         DATETIME(6)    NULL,
    due_at                 DATETIME(6)    NULL,
    consumed_hours         DECIMAL(10, 2) NULL,
    comment                TEXT           NULL,
    internal_comment       TEXT           NULL,
    return_reason          VARCHAR(500)   NULL,
    suspended_at           DATETIME(6)    NULL,
    resumed_at             DATETIME(6)    NULL,
    created_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_passage_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_passage_step
        FOREIGN KEY (chain_step_template_id) REFERENCES chain_step_templates(id),
    CONSTRAINT fk_passage_responsible
        FOREIGN KEY (responsible_user_id) REFERENCES users(id),
    UNIQUE KEY uk_passage_file_step (file_id, step_order),
    INDEX idx_passages_file (file_id),
    INDEX idx_passages_status (status),
    INDEX idx_passages_responsible (responsible_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS file_passages;
