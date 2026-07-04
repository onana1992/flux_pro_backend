package com.nanotech.flux_pro_backend.job;

import com.nanotech.flux_pro_backend.service.AlertEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche l'évaluation des règles d'alerte (ALR-01 à ALR-04) toutes les 30 minutes,
 * en heures ouvrées (7h-18h, lun-ven, Africa/Douala). Fréquence configurable via
 * fluxpro.alerts.scheduler.cron sans redéploiement du code métier.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertSchedulerJob {

    private final AlertEngineService alertEngineService;

    @Scheduled(cron = "${fluxpro.alerts.scheduler.cron:0 0/30 7-18 * * MON-FRI}", zone = "Africa/Douala")
    public void run() {
        try {
            alertEngineService.evaluateAll();
        } catch (Exception e) {
            log.error("ALR: échec de l'évaluation planifiée des alertes", e);
        }
    }
}
