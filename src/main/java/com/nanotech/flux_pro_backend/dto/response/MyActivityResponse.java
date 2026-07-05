package com.nanotech.flux_pro_backend.dto.response;

import java.util.List;

/** Widget « Mon activité » (DSH-01) — jamais filtré par périmètre organisationnel. */
public record MyActivityResponse(
        long activeCount,
        long overdueCount,
        long transmittedRecentCount,
        List<MyActivityItemResponse> items) {
}
