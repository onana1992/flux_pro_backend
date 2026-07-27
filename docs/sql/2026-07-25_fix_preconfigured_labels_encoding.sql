-- Objectif : corriger libellés préconfigurés (mojibake accents / tirets)
-- Exécution : mysql --default-character-set=utf8mb4 …

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ── Remplacements génériques (si présents) ───────────────────────────────────
UPDATE preconfigured_dossiers
SET
    name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name,
        'ÔÇö', ' - '),
        '├®', 'é'),
        '├¿', 'è'),
        '├┤', 'ô'),
        '├á', 'à'),
        '├®', 'é'),
    description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(description,
        'ÔÇö', ' - '),
        '├®', 'é'),
        '├¿', 'è'),
        '├┤', 'ô'),
        '├á', 'à'),
        '├®', 'é'),
    updated_at = NOW(6)
WHERE name LIKE '%├%' OR name LIKE '%ÔÇö%'
   OR description LIKE '%├%' OR description LIKE '%ÔÇö%';

-- ── Valeurs canoniques (source de vérité) ────────────────────────────────────
UPDATE preconfigured_dossiers SET
    name = 'Demande de congé',
    description = 'Demande de congé agents - portail interne (pilote MVP). Circuit T-CONGE.',
    updated_at = NOW(6)
WHERE code = 'RH-CONGE';

UPDATE preconfigured_dossiers SET
    name = 'Signalement urgence routière',
    description = 'Portail externe - signalement dégradation / urgence. Circuit T-ENT-URG.',
    updated_at = NOW(6)
WHERE code = 'ENT-URG';

UPDATE preconfigured_dossiers SET
    name = 'Décompte / demande de paiement',
    description = 'Portail externe - dépôts de décomptes entrepreneurs. Circuit T-DECOMPTE.',
    updated_at = NOW(6)
WHERE code = 'DECOMPTE';

UPDATE preconfigured_dossiers SET
    name = 'Autorisation travaux domaine public',
    description = 'Portail externe - autorisation de travaux sur le domaine public routier. Circuit T-AUTH-TRAV.',
    updated_at = NOW(6)
WHERE code = 'AUTH-TRAV';

UPDATE preconfigured_dossiers SET
    name = 'Demande d''audience',
    description = 'Portail externe - demande d''audience.',
    updated_at = NOW(6)
WHERE code = 'AUD-CAB';

UPDATE preconfigured_dossiers SET
    name = 'Demande d''inspection chantier',
    description = 'Portail externe - inspection chantier.',
    updated_at = NOW(6)
WHERE code = 'INSP-CHANT';

UPDATE preconfigured_dossiers SET
    name = 'Autorisation occupation temporaire',
    description = 'Portail externe - occupation temporaire.',
    updated_at = NOW(6)
WHERE code = 'AUTH-OCC';

UPDATE preconfigured_dossiers SET
    name = 'Déclaration ouverture / reprise chantier',
    description = 'Portail externe - déclaration chantier.',
    updated_at = NOW(6)
WHERE code = 'DECL-CHANT';

UPDATE preconfigured_dossiers SET
    name = 'Demande d''avis technique',
    description = 'Portail externe - avis technique.',
    updated_at = NOW(6)
WHERE code = 'AVIS-TECH';

UPDATE preconfigured_dossiers SET
    name = 'Ordre de mission',
    description = 'Portail interne - ordre de mission.',
    updated_at = NOW(6)
WHERE code = 'MISSION';

UPDATE preconfigured_dossiers SET
    name = 'Attestation de service',
    description = 'Portail interne - attestation.',
    updated_at = NOW(6)
WHERE code = 'ATT-SERV';

UPDATE preconfigured_dossiers SET
    name = 'Réclamation / contentieux',
    description = 'Portail externe - réclamation.',
    updated_at = NOW(6)
WHERE code = 'RECLAM';

-- ── Aligner file_types liés ──────────────────────────────────────────────────
UPDATE file_types SET name = 'Demande de congé', updated_at = NOW(6) WHERE code = 'RH-CONGE';
UPDATE file_types SET name = 'Signalement urgence routière', updated_at = NOW(6) WHERE code = 'ENT-URG';
UPDATE file_types SET name = 'Décompte / demande de paiement', updated_at = NOW(6) WHERE code = 'DECOMPTE';
UPDATE file_types SET name = 'Autorisation travaux domaine public', updated_at = NOW(6) WHERE code = 'AUTH-TRAV';
UPDATE file_types SET name = 'Demande d''audience', updated_at = NOW(6) WHERE code = 'AUD-CAB';
UPDATE file_types SET name = 'Demande d''inspection chantier', updated_at = NOW(6) WHERE code = 'INSP-CHANT';
UPDATE file_types SET name = 'Autorisation occupation temporaire', updated_at = NOW(6) WHERE code = 'AUTH-OCC';
UPDATE file_types SET name = 'Déclaration ouverture / reprise chantier', updated_at = NOW(6) WHERE code = 'DECL-CHANT';
UPDATE file_types SET name = 'Demande d''avis technique', updated_at = NOW(6) WHERE code = 'AVIS-TECH';
UPDATE file_types SET name = 'Ordre de mission', updated_at = NOW(6) WHERE code = 'MISSION';
UPDATE file_types SET name = 'Attestation de service', updated_at = NOW(6) WHERE code = 'ATT-SERV';
UPDATE file_types SET name = 'Réclamation / contentieux', updated_at = NOW(6) WHERE code = 'RECLAM';

-- ── Aligner templates de chaîne associés ─────────────────────────────────────
UPDATE chain_templates SET name = 'Demande de congé (RH)', updated_at = NOW(6) WHERE code = 'T-CONGE';
UPDATE chain_templates SET name = 'Urgence routière DRTP', updated_at = NOW(6) WHERE code = 'T-ENT-URG';
UPDATE chain_templates SET name = 'Décompte entrepreneur', updated_at = NOW(6) WHERE code = 'T-DECOMPTE';
UPDATE chain_templates SET name = 'Réclamation', updated_at = NOW(6) WHERE code = 'T-RECLAM';

UPDATE chain_templates
SET
    name = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(name,
        'ÔÇö', ' - '), '├®', 'é'), '├¿', 'è'), '├┤', 'ô'), '├á', 'à'),
    description = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(IFNULL(description,''),
        'ÔÇö', ' - '), '├®', 'é'), '├¿', 'è'), '├┤', 'ô'), '├á', 'à'),
    updated_at = NOW(6)
WHERE name LIKE '%├%' OR name LIKE '%ÔÇö%'
   OR IFNULL(description,'') LIKE '%├%' OR IFNULL(description,'') LIKE '%ÔÇö%';

UPDATE chain_step_templates
SET
    label = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(label,
        'ÔÇö', ' - '), '├®', 'é'), '├¿', 'è'), '├┤', 'ô'), '├á', 'à'),
    expected_action = REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(IFNULL(expected_action,''),
        'ÔÇö', ' - '), '├®', 'é'), '├¿', 'è'), '├┤', 'ô'), '├á', 'à'),
    updated_at = NOW(6)
WHERE label LIKE '%├%' OR label LIKE '%ÔÇö%'
   OR IFNULL(expected_action,'') LIKE '%├%' OR IFNULL(expected_action,'') LIKE '%ÔÇö%';

SELECT code, name, LEFT(description, 70) AS description
FROM preconfigured_dossiers
ORDER BY code;
