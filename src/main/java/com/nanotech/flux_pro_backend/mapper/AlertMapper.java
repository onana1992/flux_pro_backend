package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.AlertResponse;
import com.nanotech.flux_pro_backend.entity.Alert;

public final class AlertMapper {

    private AlertMapper() {
    }

    public static AlertResponse toResponse(Alert alert) {
        String fileReferenceNumber = alert.getFile() != null ? alert.getFile().getReferenceNumber() : null;
        String stepLabel = alert.getFilePassage() != null
                ? alert.getFilePassage().getChainStepTemplate().getLabel()
                : null;
        return new AlertResponse(
                alert.getId(),
                alert.getFile() != null ? alert.getFile().getId() : null,
                fileReferenceNumber,
                alert.getFilePassage() != null ? alert.getFilePassage().getId() : null,
                stepLabel,
                alert.getAlertType().getCode(),
                alert.getAlertType().getLabel(),
                alert.getEscalationLevel(),
                alert.getChannel(),
                alert.getStatus(),
                alert.getSentAt(),
                alert.getReadAt(),
                buildMessage(alert, fileReferenceNumber, stepLabel));
    }

    private static String buildMessage(Alert alert, String fileReferenceNumber, String stepLabel) {
        StringBuilder sb = new StringBuilder(alert.getAlertType().getLabel());
        if (fileReferenceNumber != null) {
            sb.append(" — ").append(fileReferenceNumber);
        }
        if (stepLabel != null) {
            sb.append(" — ").append(stepLabel);
        }
        return sb.toString();
    }
}
