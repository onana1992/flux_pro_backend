package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.enumeration.UserRole;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Déclenche les jobs ALR en s'appuyant sur {@link ClockService#now()} (heure réelle
 * ou simulée), et non sur l'horloge mur seule.
 * <p>
 * Un poll mur (par défaut 5 s) détecte les créneaux cron franchis par le tick simulé ;
 * un saut calendaire (set/adjust) publie {@link SystemClockMovedEvent} pour un rattrapage immédiat.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClockDrivenJobScheduler {

    private static final int MAX_FIRES_PER_JOB_PER_CATCHUP = 64;

    private final ClockService clockService;
    private final AlertEngineService alertEngineService;

    @Value("${fluxpro.alerts.scheduler.cron:0 0/30 7-18 * * MON-FRI}")
    private String alertEngineCron;

    @Value("${fluxpro.alerts.digest.cron:0 30 7 * * MON-FRI}")
    private String digestCron;

    @Value("${fluxpro.alerts.digest.target-role:DIRECTOR}")
    private String digestTargetRole;

    private final AtomicReference<Instant> cursor = new AtomicReference<>();

    private CronExpression alertCronExpression;
    private CronExpression digestCronExpression;

    @PostConstruct
    void init() {
        alertCronExpression = CronExpression.parse(alertEngineCron);
        digestCronExpression = CronExpression.parse(digestCron);
        Instant now = clockService.now();
        cursor.set(now);
        log.info(
                "ClockDrivenJobScheduler prêt — curseur={}, alertCron={}, digestCron={}",
                now, alertEngineCron, digestCron);
    }

    /** Poll mur : suit le tick de l'horloge (réelle ou simulée). */
    @Scheduled(fixedDelayString = "${fluxpro.clock.job-poll-ms:5000}")
    public void poll() {
        catchUp();
    }

    /**
     * Rattrapage hors thread HTTP : sinon le POST /clock/set bloque plusieurs secondes
     * (jobs + SMTP) et le front laisse l'horloge « disabled » jusqu'au timeout.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClockMoved(SystemClockMovedEvent event) {
        log.info(
                "Horloge déplacée {} → {} — rattrapage async des jobs cron",
                event.previousSimulatedNow(), event.newSimulatedNow());
        catchUp();
    }

    /**
     * Exécute chaque occurrence cron dont l'instant simulé est dans (cursor, now].
     */
    public synchronized void catchUp() {
        Instant now = clockService.now();
        Instant from = cursor.get();
        if (from == null) {
            cursor.set(now);
            return;
        }
        if (now.isBefore(from)) {
            log.info("Recul d'horloge simulée détecté — curseur jobs réinitialisé à {}", now);
            cursor.set(now);
            return;
        }
        if (!now.isAfter(from)) {
            return;
        }

        fireBetween(alertCronExpression, "alert-engine", from, now, alertEngineService::evaluateAll);
        fireBetween(
                digestCronExpression,
                "daily-digest",
                from,
                now,
                () -> alertEngineService.runDailyDigest(UserRole.valueOf(digestTargetRole)));

        cursor.set(now);
    }

    private void fireBetween(
            CronExpression cron, String jobName, Instant from, Instant toInclusive, Runnable job) {
        ZonedDateTime cursorZ = from.atZone(ClockService.BUSINESS_ZONE);
        ZonedDateTime endZ = toInclusive.atZone(ClockService.BUSINESS_ZONE);
        int fires = 0;
        ZonedDateTime next = cron.next(cursorZ);
        while (next != null && !next.isAfter(endZ) && fires < MAX_FIRES_PER_JOB_PER_CATCHUP) {
            log.info("Job '{}' déclenché à l'heure simulée {}", jobName, next);
            try {
                job.run();
            } catch (Exception e) {
                log.error("Job '{}' a échoué (heure simulée {})", jobName, next, e);
            }
            fires++;
            next = cron.next(next);
        }
        if (fires >= MAX_FIRES_PER_JOB_PER_CATCHUP) {
            log.warn(
                    "Job '{}' : plafond de {} déclenchements atteint sur ce saut — créneaux restants ignorés",
                    jobName,
                    MAX_FIRES_PER_JOB_PER_CATCHUP);
        } else if (fires > 0) {
            log.info("Job '{}' : {} exécution(s) rattrapée(s)", jobName, fires);
        }
    }
}
