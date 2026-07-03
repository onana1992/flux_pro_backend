package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.ChainStepTemplate;
import com.nanotech.flux_pro_backend.entity.ChainTemplate;
import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.repository.ChainTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class ChainDataInitializer implements CommandLineRunner {

    private final ChainTemplateRepository chainTemplateRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedIfAbsent("T01", this::buildT01);
            seedIfAbsent("T02", this::buildT02);
            seedIfAbsent("T03", this::buildT03);
            seedIfAbsent("T04", this::buildT04);
            seedIfAbsent("T05", this::buildT05);
            log.info("Chain template reference data initialized");
        } catch (Exception e) {
            log.warn("Chain template initialization skipped — execute docs/sql/2026-07-02_chain_templates.sql: {}",
                    e.getMessage());
        }
    }

    private void seedIfAbsent(String code, java.util.function.Supplier<ChainTemplate> builder) {
        if (chainTemplateRepository.findByCodeIgnoreCase(code).isEmpty()) {
            chainTemplateRepository.save(builder.get());
        }
    }

    private ChainTemplate buildT01() {
        ChainTemplate t = baseTemplate("T01", "Courrier entrant standard",
                "Circuit standard pour courrier entrant MINTP", "COUR-STD", 11, DelayUnit.WORKING_DAYS, true);
        addStep(t, 1, "Réception DAG", UserRole.SUPPORT, 1, DelayUnit.WORKING_DAYS,
                "Enregistrer et numériser le courrier", false, false);
        addStep(t, 2, "Orientation Chef Courrier", UserRole.SERVICE_HEAD, 1, DelayUnit.WORKING_DAYS,
                "Orienter vers direction", false, false);
        addStep(t, 3, "Directeur destinataire", UserRole.DIRECTOR, 1, DelayUnit.WORKING_DAYS,
                "Désigner agent traitant", false, false);
        addStep(t, 4, "Agent traitant", UserRole.AGENT, 5, DelayUnit.WORKING_DAYS,
                "Traiter et préparer réponse", false, false);
        addStep(t, 5, "Validation Chef Service", UserRole.SERVICE_HEAD, 2, DelayUnit.WORKING_DAYS,
                "Valider le projet de réponse", false, false);
        addStep(t, 6, "Expédition réponse", UserRole.SUPPORT, 1, DelayUnit.WORKING_DAYS,
                "Expédier et archiver scan", false, false);
        addStep(t, 7, "Clôture", UserRole.AGENT, 0, DelayUnit.WORKING_DAYS,
                "Clôturer le dossier", false, true);
        return t;
    }

    private ChainTemplate buildT02() {
        ChainTemplate t = baseTemplate("T02", "Courrier très urgent",
                "Circuit accéléré pour courrier urgent", "COUR-URG", 3, DelayUnit.WORKING_HOURS, true);
        addStep(t, 1, "Réception DAG", UserRole.SUPPORT, 4, DelayUnit.WORKING_HOURS,
                "Enregistrer et numériser", false, false);
        addStep(t, 2, "Directeur destinataire", UserRole.DIRECTOR, 4, DelayUnit.WORKING_HOURS,
                "Désigner agent traitant", false, false);
        addStep(t, 3, "Agent traitant", UserRole.AGENT, 1, DelayUnit.WORKING_DAYS,
                "Traiter et préparer réponse", false, false);
        addStep(t, 4, "Validation", UserRole.SERVICE_HEAD, 4, DelayUnit.WORKING_HOURS,
                "Valider le projet", false, false);
        addStep(t, 5, "Expédition", UserRole.SUPPORT, 4, DelayUnit.WORKING_HOURS,
                "Expédier et archiver", false, false);
        addStep(t, 6, "Clôture", UserRole.AGENT, 0, DelayUnit.WORKING_DAYS,
                "Clôturer le dossier", false, true);
        return t;
    }

    private ChainTemplate buildT03() {
        ChainTemplate t = baseTemplate("T03", "Marché public simplifié",
                "Circuit marchés publics simplifiés DIER", "MARCHE-SMP", 21, DelayUnit.WORKING_DAYS, true);
        addStep(t, 1, "Enregistrement et visa SG", UserRole.SUPPORT, 1, DelayUnit.WORKING_DAYS,
                "Enregistrer le dossier", false, false);
        addStep(t, 2, "Instruction technique", UserRole.AGENT, 5, DelayUnit.WORKING_DAYS,
                "Instruire techniquement", false, false);
        addStep(t, 3, "Visa financier", UserRole.SERVICE_HEAD, 3, DelayUnit.WORKING_DAYS,
                "Visa financier", false, false);
        addStep(t, 4, "Validation Directeur DIER", UserRole.DIRECTOR, 2, DelayUnit.WORKING_DAYS,
                "Valider l'instruction", false, false);
        addStep(t, 5, "Avis SG", UserRole.SECRETARY_GENERAL, 3, DelayUnit.WORKING_DAYS,
                "Avis secrétaire général", false, false);
        addStep(t, 6, "Visa Ministre (si requis)", UserRole.EXECUTIVE_OFFICE, 5, DelayUnit.WORKING_DAYS,
                "Visa ministre si requis", true, false);
        addStep(t, 7, "Notification et archivage", UserRole.SUPPORT, 2, DelayUnit.WORKING_DAYS,
                "Notifier et archiver", false, false);
        addStep(t, 8, "Clôture", UserRole.AGENT, 0, DelayUnit.WORKING_DAYS,
                "Clôturer le dossier", false, true);
        return t;
    }

    private ChainTemplate buildT04() {
        ChainTemplate t = baseTemplate("T04", "Autorisation travaux DRTP",
                "Circuit autorisation de travaux DRTP", "AUTH-TRAV", 18, DelayUnit.WORKING_DAYS, true);
        addStep(t, 1, "Réception demande", UserRole.SUPPORT, 1, DelayUnit.WORKING_DAYS,
                "Enregistrer la demande", false, false);
        addStep(t, 2, "Instruction technique", UserRole.AGENT, 7, DelayUnit.WORKING_DAYS,
                "Instruire techniquement", false, false);
        addStep(t, 3, "Visite terrain", UserRole.SERVICE_HEAD, 5, DelayUnit.WORKING_DAYS,
                "Organiser visite terrain", false, false);
        addStep(t, 4, "Validation Directeur DRTP", UserRole.REGIONAL_DIRECTOR, 3, DelayUnit.WORKING_DAYS,
                "Valider l'instruction", false, false);
        addStep(t, 5, "Délivrance autorisation", UserRole.SUPPORT, 2, DelayUnit.WORKING_DAYS,
                "Délivrer l'autorisation", false, false);
        addStep(t, 6, "Clôture", UserRole.AGENT, 0, DelayUnit.WORKING_DAYS,
                "Clôturer le dossier", false, true);
        return t;
    }

    private ChainTemplate buildT05() {
        ChainTemplate t = baseTemplate("T05", "Coopération / partenariat",
                "Circuit coopération internationale — hors pilote", "COOP-PART", 10, DelayUnit.WORKING_DAYS, true);
        t.setActive(false);
        addStep(t, 1, "Réception dossier", UserRole.SUPPORT, 2, DelayUnit.WORKING_DAYS,
                "Enregistrer le dossier", false, false);
        addStep(t, 2, "Instruction", UserRole.AGENT, 8, DelayUnit.WORKING_DAYS,
                "Instruire le dossier", false, false);
        addStep(t, 3, "Clôture", UserRole.AGENT, 0, DelayUnit.WORKING_DAYS,
                "Clôturer le dossier", false, true);
        return t;
    }

    private ChainTemplate baseTemplate(
            String code,
            String name,
            String description,
            String fileTypeCode,
            int totalDelayDays,
            DelayUnit delayUnit,
            boolean system) {
        ChainTemplate t = new ChainTemplate();
        t.setCode(code);
        t.setName(name);
        t.setDescription(description);
        t.setFileTypeCode(fileTypeCode);
        t.setTotalDelayDays(totalDelayDays);
        t.setDelayUnit(delayUnit);
        t.setActive(true);
        t.setSystemTemplate(system);
        return t;
    }

    private void addStep(
            ChainTemplate template,
            int order,
            String label,
            UserRole role,
            int delay,
            DelayUnit unit,
            String action,
            boolean optional,
            boolean closure) {
        ChainStepTemplate step = new ChainStepTemplate();
        step.setChainTemplate(template);
        step.setStepOrder(order);
        step.setLabel(label);
        step.setResponsibleRole(role);
        step.setDelayValue(delay);
        step.setDelayUnit(unit);
        step.setExpectedAction(action);
        step.setOptional(optional);
        step.setClosureStep(closure);
        template.getSteps().add(step);
    }
}
