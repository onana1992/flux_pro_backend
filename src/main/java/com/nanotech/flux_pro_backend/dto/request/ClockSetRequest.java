package com.nanotech.flux_pro_backend.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ClockSetRequest(@NotNull Instant instant) {
}
