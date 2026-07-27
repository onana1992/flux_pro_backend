-- Objectif : permettre le reseed des maillons sans organization_id, puis backfill
-- À exécuter AVANT les scripts seed_*circuits / seed_portal_* qui réinsèrent des steps

ALTER TABLE chain_step_templates
    DROP FOREIGN KEY fk_step_template_organization;

ALTER TABLE chain_step_templates
    MODIFY organization_id BINARY(16) NULL;
