package com.nanotech.flux_pro_backend.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record BusinessCalendarDayResponse(
        UUID id,
        LocalDate calendarDate,
        String label,
        String countryCode) {
}
