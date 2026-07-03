package com.nanotech.flux_pro_backend.dto.response;

import java.util.List;

public record FilePassageCircuitResponse(
        String templateCode,
        String templateName,
        Integer currentStepOrder,
        CurrentHolderResponse currentHolder,
        List<PassageResponse> passages
) {
}
