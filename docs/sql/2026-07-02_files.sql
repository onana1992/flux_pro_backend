-- Objectif : module DOS — dossiers administratifs (files, pièces jointes, numérotation)
-- Tables impactées : files, file_attachments, file_number_sequences (création)
-- Prérequis : users, organization, chain_templates, file_types ; UUID BINARY(16)
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)

CREATE TABLE IF NOT EXISTS files (
    id                      BINARY(16)   NOT NULL PRIMARY KEY,
    reference_number        VARCHAR(32)  NULL,
    file_type_code          VARCHAR(32)  NOT NULL,
    chain_template_id       BINARY(16)   NULL,
    organization_id         BINARY(16)   NOT NULL,
    created_by_user_id      BINARY(16)   NOT NULL,
    subject                 VARCHAR(500) NOT NULL,
    sender_or_beneficiary   VARCHAR(255) NOT NULL,
    received_at             DATE         NOT NULL,
    priority                VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    status                  VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    closure_reason          TEXT         NULL,
    closed_at               DATETIME(6)  NULL,
    cancelled_at            DATETIME(6)  NULL,
    cancellation_reason     TEXT         NULL,
    external_hold_reason    TEXT         NULL,
    external_hold_since     DATETIME(6)  NULL,
    metadata                LONGTEXT     NULL,
    created_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at              DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_files_reference UNIQUE (reference_number),
    CONSTRAINT fk_files_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_files_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_files_chain_template
        FOREIGN KEY (chain_template_id) REFERENCES chain_templates(id),
    INDEX idx_files_org (organization_id),
    INDEX idx_files_status (status),
    INDEX idx_files_type (file_type_code),
    INDEX idx_files_received (received_at),
    INDEX idx_files_ref (reference_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_number_sequences (
    organization_id BINARY(16) NOT NULL,
    year            INT         NOT NULL,
    last_sequence   INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (organization_id, year),
    CONSTRAINT fk_seq_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS file_attachments (
    id                BINARY(16)   NOT NULL PRIMARY KEY,
    file_id           BINARY(16)   NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type      VARCHAR(100) NOT NULL,
    size_bytes        BIGINT       NOT NULL,
    storage_bucket    VARCHAR(63)  NOT NULL,
    storage_key       VARCHAR(512) NOT NULL,
    response_document BOOLEAN      NOT NULL DEFAULT FALSE,
    uploaded_by_id    BINARY(16)   NOT NULL,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_attachment_file
        FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT fk_attachment_user
        FOREIGN KEY (uploaded_by_id) REFERENCES users(id),
    INDEX idx_attachments_file (file_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS file_attachments;
-- DROP TABLE IF EXISTS file_number_sequences;
-- DROP TABLE IF EXISTS files;
