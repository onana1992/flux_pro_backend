package com.nanotech.flux_pro_backend.dto.response;

import java.time.LocalDate;

/** Point de série temporelle pour l'analyse BI (créés / clôturés / conformes). */
public record AnalyticsTrendPointResponse(
        LocalDate date,
        long createdCount,
        long closedCount,
        long compliantClosedCount) {
}
