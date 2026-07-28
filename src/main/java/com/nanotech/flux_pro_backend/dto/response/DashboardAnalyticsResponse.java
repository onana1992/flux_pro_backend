package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.DashboardScopeWidth;

import java.util.List;
import java.util.UUID;

/**
 * Payload d'analyse BI pour {@code /api/dashboard/analytics} —
 * KPI + tendances + breakdowns (pas un export).
 */
public record DashboardAnalyticsResponse(
        UUID organizationId,
        String organizationCode,
        DashboardScopeWidth scopeWidth,
        int windowDays,
        String granularity,
        long activeFiles,
        long overdueFiles,
        long closedInWindow,
        long createdInWindow,
        double complianceRate,
        double averageDelayDays,
        List<AnalyticsTrendPointResponse> volumeTrend,
        List<AnalyticsStatusSliceResponse> statusBreakdown,
        List<DelayByTypeResponse> delayByType,
        List<OrganizationRankingResponse> complianceRanking,
        List<OverdueFileResponse> overdueFilesList,
        List<WorkloadEntryResponse> workload) {
}
