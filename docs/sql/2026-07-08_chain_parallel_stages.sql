-- Objectif : autoriser plusieurs maillons parallèles par étape (join AND)
-- Tables impactées : chain_step_templates, file_passages
-- Prérequis : schéma CHN-TPL / CHN-PASS existant
-- Exécution : manuelle sur MySQL avant déploiement (ddl-auto=none)
--
-- step_order devient le numéro d'ÉTAPE (plusieurs maillons peuvent partager la même valeur).
-- La jonction entre étapes est de type AND : l'étape suivante n'est activée que lorsque
-- tous les maillons de l'étape courante sont COMPLETED.

ALTER TABLE chain_step_templates
    DROP INDEX uk_chain_step_order;

ALTER TABLE file_passages
    DROP INDEX uk_passage_file_step;

ALTER TABLE file_passages
    ADD CONSTRAINT uk_passage_file_step_template UNIQUE (file_id, chain_step_template_id);

-- rollback (commenté) :
-- ALTER TABLE file_passages DROP INDEX uk_passage_file_step_template;
-- ALTER TABLE file_passages ADD UNIQUE KEY uk_passage_file_step (file_id, step_order);
-- ALTER TABLE chain_step_templates ADD UNIQUE KEY uk_chain_step_order (chain_template_id, step_order);
