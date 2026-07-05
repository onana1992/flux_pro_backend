package com.nanotech.flux_pro_backend.dto.response;

import java.util.UUID;

/** Charge par agent (DSH-02), triée par retards décroissants côté service. */
public record WorkloadEntryResponse(
        UUID userId,
        String firstName,
        String lastName,
        String organizationCode,
        long activeCount,
        long overdueCount) {
}
