-- Objectif : template de chaîne de démonstration avec maillons parallèles (join AND)
-- Tables impactées : chain_templates, chain_step_templates
-- Prérequis :
--   - docs/sql/2026-07-02_chain_templates.sql
--   - docs/sql/2026-07-08_chain_parallel_stages.sql (contrainte step_order non unique)
--   - type de dossier MARCHE-SMP présent (FileTypeDataInitializer ou seed métier)
-- Exécution : manuelle sur MySQL / MariaDB (ddl-auto=none)
-- Idempotent : ré-exécutable (template T06 créé une fois ; maillons remplacés à chaque run)
--
-- Circuit T06 — « Marché — visas parallèles »
--   Étape 1 : Enregistrement DAG
--   Étape 2 : Visa technique DIER  ║  Visa financier DAF  ║  Avis juridique  (parallèle, join AND)
--   Étape 3 : Validation Secrétaire Général
--   Étape 4 : Clôture
-- Délai total déclaré : 8 j.o. (= 1 + max(5,3,4) + 2)

SET @tpl_id := UNHEX(REPLACE('d1000000-0000-4000-8000-000000000601', '-', ''));

INSERT INTO chain_templates (
    id, code, name, description, file_type_code,
    total_delay_days, delay_unit, active, system_template,
    created_at, updated_at
)
SELECT
    @tpl_id,
    'T06',
    'Marché — visas parallèles',
    'Circuit de démonstration : trois visas simultanés (join AND) avant validation SG.',
    'MARCHE-SMP',
    8,
    'WORKING_DAYS',
    TRUE,
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM chain_templates WHERE code = 'T06');

-- Réaligner l''ID si le template existait déjà avec un autre UUID
SET @tpl_id := (SELECT id FROM chain_templates WHERE code = 'T06' LIMIT 1);

DELETE FROM chain_step_templates WHERE chain_template_id = @tpl_id;

INSERT INTO chain_step_templates (
    id, chain_template_id, step_order, label, responsible_role,
    delay_value, delay_unit, expected_action, optional, closure_step,
    created_at, updated_at
) VALUES
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000611', '-', '')),
        @tpl_id, 1, 'Enregistrement DAG', 'SUPPORT',
        1, 'WORKING_DAYS', 'Enregistrer le dossier et affecter le numéro', FALSE, FALSE,
        NOW(6), NOW(6)
    ),
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000621', '-', '')),
        @tpl_id, 2, 'Visa technique DIER', 'SERVICE_HEAD',
        5, 'WORKING_DAYS', 'Instruire et viser techniquement', FALSE, FALSE,
        NOW(6), NOW(6)
    ),
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000622', '-', '')),
        @tpl_id, 2, 'Visa financier DAF', 'DIRECTOR',
        3, 'WORKING_DAYS', 'Viser le volet financier et budgétaire', FALSE, FALSE,
        NOW(6), NOW(6)
    ),
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000623', '-', '')),
        @tpl_id, 2, 'Avis juridique', 'AGENT',
        4, 'WORKING_DAYS', 'Émettre l''avis juridique', FALSE, FALSE,
        NOW(6), NOW(6)
    ),
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000631', '-', '')),
        @tpl_id, 3, 'Validation Secrétaire Général', 'SECRETARY_GENERAL',
        2, 'WORKING_DAYS', 'Valider l''instruction consolidée', FALSE, FALSE,
        NOW(6), NOW(6)
    ),
    (
        UNHEX(REPLACE('d1000000-0000-4000-8000-000000000641', '-', '')),
        @tpl_id, 4, 'Clôture', 'AGENT',
        0, 'WORKING_DAYS', 'Clôturer le dossier', FALSE, TRUE,
        NOW(6), NOW(6)
    );

-- Vérification (optionnel) :
-- SELECT ct.code, cst.step_order, cst.label, cst.responsible_role, cst.delay_value
-- FROM chain_templates ct
-- JOIN chain_step_templates cst ON cst.chain_template_id = ct.id
-- WHERE ct.code = 'T06'
-- ORDER BY cst.step_order, cst.label;

-- rollback (commenté) :
-- DELETE FROM chain_step_templates WHERE chain_template_id = (SELECT id FROM chain_templates WHERE code = 'T06');
-- DELETE FROM chain_templates WHERE code = 'T06';
