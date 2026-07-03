-- Objectif : retirer default_chain_template_code de file_types
-- Le lien type → template se fait via chain_templates.file_type_code (CHN-TPL), pas l'inverse.
-- Prérequis : table file_types existante
-- Exécution : manuelle sur MySQL si la colonne a déjà été créée

ALTER TABLE file_types
    DROP COLUMN IF EXISTS default_chain_template_code;

-- rollback (commenté) :
-- ALTER TABLE file_types ADD COLUMN default_chain_template_code VARCHAR(10) NULL AFTER direction_code;
