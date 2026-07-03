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

    private DelaiService delaiService;

    @BeforeEach
    void setUp() {
        delaiService = new DelaiService(businessCalendarDayRepository);
        when(businessCalendarDayRepository.findHolidayDatesBetween(eq("CM"), any(), any()))
                .thenReturn(List.of());
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
}
