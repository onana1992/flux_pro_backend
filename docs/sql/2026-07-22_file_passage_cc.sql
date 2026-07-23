-- Objectif : copies informées (CC) par maillon — CHN-09
-- Tables impactées : file_passage_cc (création)
-- Prérequis : file_passages, users
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)

CREATE TABLE IF NOT EXISTS file_passage_cc (
    file_passage_id BINARY(16) NOT NULL,
    user_id         BINARY(16) NOT NULL,
    PRIMARY KEY (file_passage_id, user_id),
    CONSTRAINT fk_passage_cc_passage
        FOREIGN KEY (file_passage_id) REFERENCES file_passages(id) ON DELETE CASCADE,
    CONSTRAINT fk_passage_cc_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_passage_cc_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- rollback (commenté) :
-- DROP TABLE IF EXISTS file_passage_cc;
