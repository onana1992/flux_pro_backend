package com.nanotech.flux_pro_backend.job;

/**
 * @deprecated Remplacé par {@link com.nanotech.flux_pro_backend.service.ClockDrivenJobScheduler},
 * qui déclenche le digest selon {@code ClockService.now()} (horloge réelle ou simulée).
 */
@Deprecated
public final class AlertDigestJob {
    private AlertDigestJob() {
    }
}
