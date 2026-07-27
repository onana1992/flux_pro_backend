-- Objectif : responsable 1er maillon sur dossier préconfiguré
-- Tables impactées : preconfigured_dossiers
-- Prérequis : preconfigured_dossiers, users
-- Exécution : manuelle (ddl-auto=none)

ALTER TABLE preconfigured_dossiers
    ADD COLUMN default_first_step_responsible_user_id BINARY(16) NULL
        AFTER chain_template_id;

ALTER TABLE preconfigured_dossiers
    ADD CONSTRAINT fk_preconfigured_dossiers_responsible
        FOREIGN KEY (default_first_step_responsible_user_id) REFERENCES users(id);

CREATE INDEX idx_preconfigured_dossiers_responsible
    ON preconfigured_dossiers (default_first_step_responsible_user_id);

-- Seed RH-CONGE : 1er utilisateur actif SERVICE_HEAD (rôle du 1er maillon T-CONGE)
UPDATE preconfigured_dossiers pd
JOIN (
    SELECT u.id
    FROM users u
    WHERE u.role = 'SERVICE_HEAD' AND u.active = TRUE
    ORDER BY u.created_at ASC
    LIMIT 1
) resp ON TRUE
SET pd.default_first_step_responsible_user_id = resp.id,
    pd.updated_at = NOW(6)
WHERE pd.code = 'RH-CONGE'
  AND pd.default_first_step_responsible_user_id IS NULL;

-- rollback (commenté) :
-- ALTER TABLE preconfigured_dossiers DROP FOREIGN KEY fk_preconfigured_dossiers_responsible;
-- DROP INDEX idx_preconfigured_dossiers_responsible ON preconfigured_dossiers;
-- ALTER TABLE preconfigured_dossiers DROP COLUMN default_first_step_responsible_user_id;
