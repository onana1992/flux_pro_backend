package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public record PassageTransmitRequest(
        String comment,
        UUID nextResponsibleUserId,
        @Valid List<PassageNextAssignmentRequest> nextAssignments
) {
}
