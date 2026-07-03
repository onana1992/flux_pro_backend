package com.nanotech.flux_pro_backend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CurrentHolderResponse(
        UUID userId,
        String fullName,
        String organizationCode,
        String stepLabel,
        int stepOrder,
        Instant since,
        int workingDaysHeld,
        boolean overdue,
        Instant dueAt
) {
}
