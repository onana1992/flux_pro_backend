package com.nanotech.flux_pro_backend.job;

import com.nanotech.flux_pro_backend.enumeration.UserRole;
import com.nanotech.flux_pro_backend.service.AlertEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Récapitulatif quotidien des retards (ALR-08) — 7h30, lun-ven, Africa/Douala.
 * Le rôle destinataire (DIRECTOR par défaut) est une propriété, pas un rôle figé dans le code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlertDigestJob {

    private final AlertEngineService alertEngineService;

    @Value("${fluxpro.alerts.digest.target-role:DIRECTOR}")
    private String targetRole;

    @Scheduled(cron = "${fluxpro.alerts.digest.cron:0 30 7 * * MON-FRI}", zone = "Africa/Douala")
    public void run() {
        try {
            alertEngineService.runDailyDigest(UserRole.valueOf(targetRole));
        } catch (Exception e) {
            log.error("ALR: échec du digest quotidien des retards", e);
        }
    }
}
