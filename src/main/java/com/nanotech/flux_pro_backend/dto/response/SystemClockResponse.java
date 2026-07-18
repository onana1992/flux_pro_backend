package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.ClockMode;

import java.time.Instant;

public record SystemClockResponse(
        ClockMode mode,
        Instant now,
        String zoneId,
        String display,
        boolean mutable,
        Instant artificialNow,
        Instant wallSyncedAt
) {
}
