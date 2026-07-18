package com.nanotech.flux_pro_backend.job;

/**
 * @deprecated Remplacé par {@link com.nanotech.flux_pro_backend.service.ClockDrivenJobScheduler},
 * qui déclenche l'évaluation selon {@code ClockService.now()} (horloge réelle ou simulée).
 * Conservé vide pour éviter de casser d'éventuelles références documentaires.
 */
@Deprecated
public final class AlertSchedulerJob {
    private AlertSchedulerJob() {
    }
}
