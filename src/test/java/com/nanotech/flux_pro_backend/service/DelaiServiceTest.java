package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.repository.BusinessCalendarDayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelaiServiceTest {

    @Mock
    private BusinessCalendarDayRepository businessCalendarDayRepository;
    @Mock
    private TenantSettingsService tenantSettingsService;

    private DelaiService delaiService;

    @BeforeEach
    void setUp() {
        delaiService = new DelaiService(businessCalendarDayRepository, tenantSettingsService);
        org.mockito.Mockito.lenient().when(businessCalendarDayRepository.findHolidayDatesBetween(eq("CM"), any(), any()))
                .thenReturn(List.of());
        org.mockito.Mockito.lenient().when(tenantSettingsService.zoneId())
                .thenReturn(DelaiService.BUSINESS_ZONE);
        org.mockito.Mockito.lenient().when(tenantSettingsService.countryCode()).thenReturn("CM");
    }

    @Test
    void calculateDueDate_fiveWorkingDays_skipsWeekend() {
        ZonedDateTime start = ZonedDateTime.of(2026, 6, 10, 9, 0, 0, 0, DelaiService.BUSINESS_ZONE);
        Instant due = delaiService.calculateDueDate(start.toInstant(), 5, DelayUnit.WORKING_DAYS);
        ZonedDateTime dueZoned = ZonedDateTime.ofInstant(due, DelaiService.BUSINESS_ZONE);

        assertThat(dueZoned.toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 17));
        assertThat(dueZoned.toLocalTime()).isEqualTo(java.time.LocalTime.of(17, 0));
    }

    @Test
    void calculateDueDate_zeroDelay_returnsNull() {
        Instant start = ZonedDateTime.of(2026, 6, 10, 9, 0, 0, 0, DelaiService.BUSINESS_ZONE).toInstant();
        assertThat(delaiService.calculateDueDate(start, 0, DelayUnit.WORKING_DAYS)).isNull();
        assertThat(delaiService.calculateDueDate(start, -1, DelayUnit.WORKING_HOURS)).isNull();
    }

    @Test
    void addWorkingHours_splitsAcrossWeekend() {
        ZonedDateTime start = ZonedDateTime.of(2026, 6, 12, 15, 0, 0, 0, DelaiService.BUSINESS_ZONE);
        ZonedDateTime due = delaiService.addWorkingHours(start, 4);

        assertThat(due.toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(due.getHour()).isEqualTo(10);
    }

    @Test
    void countWorkingDays_returnsAtLeastOneForSameDay() {
        Instant start = ZonedDateTime.of(2026, 6, 10, 9, 0, 0, 0, ZoneId.of("Africa/Douala")).toInstant();
        Instant end = ZonedDateTime.of(2026, 6, 10, 16, 0, 0, 0, ZoneId.of("Africa/Douala")).toInstant();
        assertThat(delaiService.countWorkingDays(start, end)).isEqualTo(1);
    }

    @Test
    void applyOffset_zero_returnsSameInstant() {
        Instant dueAt = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, DelaiService.BUSINESS_ZONE).toInstant();
        assertThat(delaiService.applyOffset(dueAt, 0, DelayUnit.WORKING_DAYS)).isEqualTo(dueAt);
    }

    @Test
    void applyOffset_negativeWorkingDays_skipsWeekendBackward() {
        // Lundi 15/06/2026 → J-2 ouvrés doit tomber jeudi 11/06/2026 (en sautant le week-end).
        Instant dueAt = ZonedDateTime.of(2026, 6, 15, 10, 0, 0, 0, DelaiService.BUSINESS_ZONE).toInstant();
        Instant threshold = delaiService.applyOffset(dueAt, -2, DelayUnit.WORKING_DAYS);
        ZonedDateTime zoned = ZonedDateTime.ofInstant(threshold, DelaiService.BUSINESS_ZONE);

        assertThat(zoned.toLocalDate()).isEqualTo(LocalDate.of(2026, 6, 11));
        assertThat(threshold).isBefore(dueAt);
    }

    @Test
    void applyOffset_positiveWorkingDays_isAfterDueDate() {
        Instant dueAt = ZonedDateTime.of(2026, 6, 10, 10, 0, 0, 0, DelaiService.BUSINESS_ZONE).toInstant();
        Instant threshold = delaiService.applyOffset(dueAt, 3, DelayUnit.WORKING_DAYS);

        assertThat(threshold).isAfter(dueAt);
    }
}
