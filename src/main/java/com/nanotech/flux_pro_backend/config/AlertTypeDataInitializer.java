package com.nanotech.flux_pro_backend.config;

import com.nanotech.flux_pro_backend.entity.AlertType;
import com.nanotech.flux_pro_backend.repository.AlertTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed des 4 types d'alerte du CDC (REMINDER/OVERDUE/ESCALATION/DAILY_DIGEST) marqués
 * systemDefined = true (protégés contre la suppression). Un admin métier peut en créer
 * d'autres librement via /api/admin/alert-types (ALR-F17).
 */
@Component
@Order(22)
@RequiredArgsConstructor
@Slf4j
public class AlertTypeDataInitializer implements CommandLineRunner {

    private final AlertTypeRepository alertTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedIfAbsent("REMINDER", "Rappel avant échéance",
                    "Rappel envoyé au responsable actuel avant l'échéance du maillon (ALR-01)",
                    "alert-reminder");
            seedIfAbsent("OVERDUE", "Dépassement d'échéance",
                    "Notification envoyée à l'échéance dépassée (ALR-02)", "alert-overdue");
            seedIfAbsent("ESCALATION", "Escalade hiérarchique",
                    "Escalade envoyée au palier hiérarchique supérieur configuré (ALR-03/ALR-04)",
                    "alert-escalation");
            seedIfAbsent("DAILY_DIGEST", "Récapitulatif quotidien des retards",
                    "Digest quotidien des dossiers en retard (ALR-08)", "alert-daily-digest");
            log.info("Alert type reference data initialized");
        } catch (Exception e) {
            log.warn("Alert type initialization skipped — execute docs/sql/2026-07-04_alert_types.sql: {}",
                    e.getMessage());
        }
    }

    private void seedIfAbsent(String code, String label, String description, String emailTemplateCode) {
        if (alertTypeRepository.findByCodeIgnoreCase(code).isEmpty()) {
            AlertType type = new AlertType();
            type.setCode(code);
            type.setLabel(label);
            type.setDescription(description);
            type.setEmailTemplateCode(emailTemplateCode);
            type.setSystemDefined(true);
            type.setActive(true);
            alertTypeRepository.save(type);
        }
    }
}
