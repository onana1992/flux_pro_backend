package com.nanotech.flux_pro_backend.dto.response;

/** Répartition des dossiers par statut (analyse BI). */
public record AnalyticsStatusSliceResponse(
        String status,
        long count) {
}
