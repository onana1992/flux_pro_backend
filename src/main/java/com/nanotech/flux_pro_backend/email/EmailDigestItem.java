package com.nanotech.flux_pro_backend.email;

/**
 * Ligne du récapitulatif quotidien des retards (ALR-08).
 */
public record EmailDigestItem(
        String fileReference,
        String fileSubject,
        String stepLabel,
        String dueAtFormatted,
        int overdueWorkingDays,
        String responsibleName,
        String fileUrl
) {
}
