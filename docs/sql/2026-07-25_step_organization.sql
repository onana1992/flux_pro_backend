-- Objectif : chaque maillon (chain_step_templates) lié à une organisation
-- Prérequis : organization, chain_step_templates
-- Exécution : manuelle MySQL/MariaDB

ALTER TABLE chain_step_templates
    ADD COLUMN organization_id BINARY(16) NULL AFTER responsible_role;

-- Backfill provisoire : org du direction_code du type de dossier, sinon MINTP
UPDATE chain_step_templates cst
INNER JOIN chain_templates ct ON ct.id = cst.chain_template_id
LEFT JOIN file_types ft ON ft.code = ct.file_type_code
LEFT JOIN organization o_dir ON o_dir.code = ft.direction_code
LEFT JOIN organization o_mintp ON o_mintp.code = 'MINTP'
SET cst.organization_id = COALESCE(o_dir.id, o_mintp.id)
WHERE cst.organization_id IS NULL;

-- Affiner quelques rôles
UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DAG-COURRIER'
SET cst.organization_id = o.id
WHERE cst.responsible_role = 'SUPPORT'
  AND cst.label LIKE '%Réception%'
  AND EXISTS (
      SELECT 1 FROM chain_templates ct
      WHERE ct.id = cst.chain_template_id
        AND ct.file_type_code IN ('COUR-STD', 'COUR-URG', 'COUR-OUT', 'RH-CONGE', 'MISSION', 'ATT-SERV')
  );

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DRTP-C'
SET cst.organization_id = o.id
WHERE cst.responsible_role = 'REGIONAL_DIRECTOR';

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'MINTP-CABINET'
SET cst.organization_id = o.id
WHERE cst.responsible_role = 'EXECUTIVE_OFFICE';

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'MINTP-SG'
SET cst.organization_id = o.id
WHERE cst.responsible_role = 'SECRETARY_GENERAL';

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DAG-RH'
SET cst.organization_id = o.id
WHERE cst.organization_id IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM chain_templates ct
      WHERE ct.id = cst.chain_template_id AND ct.file_type_code = 'RH-CONGE'
  )
  AND cst.responsible_role IN ('SERVICE_HEAD', 'AGENT', 'DIRECTOR');

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DRTP-C-AUTH'
SET cst.organization_id = o.id
WHERE EXISTS (
      SELECT 1 FROM chain_templates ct
      WHERE ct.id = cst.chain_template_id AND ct.file_type_code = 'AUTH-TRAV'
  )
  AND cst.responsible_role = 'AGENT'
  AND cst.label LIKE '%Instruction%';

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DRTP-C'
SET cst.organization_id = o.id
WHERE cst.responsible_role = 'REGIONAL_DIRECTOR';

UPDATE chain_step_templates cst
INNER JOIN organization o ON o.code = 'DIER'
SET cst.organization_id = o.id
WHERE EXISTS (
      SELECT 1 FROM chain_templates ct
      WHERE ct.id = cst.chain_template_id
        AND ct.file_type_code IN ('ENT-URG', 'DECOMPTE', 'MARCHE-SMP')
  )
  AND cst.responsible_role = 'DIRECTOR';

-- Rendre NOT NULL si tout est rempli
UPDATE chain_step_templates
SET organization_id = (SELECT id FROM organization WHERE code = 'MINTP' LIMIT 1)
WHERE organization_id IS NULL;

ALTER TABLE chain_step_templates
    MODIFY organization_id BINARY(16) NOT NULL,
    ADD CONSTRAINT fk_step_template_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id);

CREATE INDEX idx_step_template_organization ON chain_step_templates (organization_id);

SELECT
    ct.code AS template,
    cst.step_order,
    cst.label,
    cst.responsible_role,
    o.code AS org
FROM chain_step_templates cst
JOIN chain_templates ct ON ct.id = cst.chain_template_id
JOIN organization o ON o.id = cst.organization_id
ORDER BY ct.code, cst.step_order
LIMIT 40;
