package com.nanotech.flux_pro_backend.dto.request;

public record PassageTransmitRequest(
        String comment,
        java.util.UUID nextResponsibleUserId
) {
}
