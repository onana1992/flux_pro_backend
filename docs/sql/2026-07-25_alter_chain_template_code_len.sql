-- Objectif : élargir chain_templates.code (était VARCHAR(10) — tronquait T-CONST-BAT, T-INSP-CHANT…)
-- Exécution : avant seeds 2026-07-25_seed_remaining_circuits_*.sql

ALTER TABLE chain_templates
    MODIFY code VARCHAR(32) NOT NULL;

-- Corriger codes tronqués s'ils existent déjà
UPDATE chain_templates SET code = 'T-CONST-BAT' WHERE code = 'T-CONST-BA';
UPDATE chain_templates SET code = 'T-INSP-CHANT' WHERE code = 'T-INSP-CHA';
UPDATE chain_templates SET code = 'T-AUTH-OCC' WHERE code = 'T-AUTH-OCC' OR code = 'T-AUTH-OC';
UPDATE chain_templates SET code = 'T-DECL-CHANT' WHERE code LIKE 'T-DECL-CH%';
UPDATE chain_templates SET code = 'T-AVIS-TECH' WHERE code LIKE 'T-AVIS-TE%';
UPDATE chain_templates SET code = 'T-PROG-INFRA' WHERE code LIKE 'T-PROG-IN%';
UPDATE chain_templates SET code = 'T-ATT-SERV' WHERE code LIKE 'T-ATT-SER%';
UPDATE chain_templates SET code = 'T-ACHAT-INT' WHERE code LIKE 'T-ACHAT-I%';
UPDATE chain_templates SET code = 'T-MISSION' WHERE code = 'T-MISSION' OR code LIKE 'T-MISSIO%';
UPDATE chain_templates SET code = 'T-RECLAM' WHERE code = 'T-RECLAM';
UPDATE chain_templates SET code = 'T-NORME' WHERE code = 'T-NORME';

SELECT code, CHAR_LENGTH(code) AS len FROM chain_templates ORDER BY code;
