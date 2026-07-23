package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BusinessCalendarDayRequest(
        @NotNull LocalDate calendarDate,
        @NotBlank @Size(max = 100) String label,
        @Size(min = 2, max = 2) String countryCode) {
}
