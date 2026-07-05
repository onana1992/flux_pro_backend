package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DashboardScopeWidth;

import java.util.UUID;

/** Compteurs de périmètre (DSH-05). */
public record DashboardSummaryResponse(
        UUID organizationId,
        String organizationCode,
        DashboardScopeWidth scopeWidth,
        long activeFiles,
        long overdueFiles,
        long closedThisMonth,
        long createdThisMonth) {
}
