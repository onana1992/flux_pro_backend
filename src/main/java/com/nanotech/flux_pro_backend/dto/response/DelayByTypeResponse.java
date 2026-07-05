package com.nanotech.flux_pro_backend.dto.response;

/** Délai moyen par type de dossier sur la fenêtre glissante demandée (DSH-06). */
public record DelayByTypeResponse(
        String fileTypeCode,
        String fileTypeLabel,
        long closedCount,
        double averageDelayDays,
        Integer targetDelayDays) {
}
