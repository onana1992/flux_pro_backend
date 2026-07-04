package com.nanotech.flux_pro_backend.dto.response;

import com.nanotech.flux_pro_backend.enumeration.AlertChannel;
import com.nanotech.flux_pro_backend.enumeration.AlertStatus;

import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID fileId,
        String fileReferenceNumber,
        UUID filePassageId,
        String stepLabel,
        String alertTypeCode,
        String alertTypeLabel,
        Integer escalationLevel,
        AlertChannel channel,
        AlertStatus status,
        Instant sentAt,
        Instant readAt,
        String message) {
}
