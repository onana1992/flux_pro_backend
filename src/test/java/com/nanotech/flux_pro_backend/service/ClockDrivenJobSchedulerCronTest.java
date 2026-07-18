package com.nanotech.flux_pro_backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClockDrivenJobSchedulerCronTest {

    private static final ZoneId ZONE = ZoneId.of("Africa/Douala");

    @Test
    void alertCron_firesEveryHalfHourInBusinessWindow() {
        CronExpression cron = CronExpression.parse("0 0/30 7-18 * * MON-FRI");
        // Lundi 13/07/2026 09:00 Douala
        ZonedDateTime from = ZonedDateTime.of(2026, 7, 13, 9, 0, 0, 0, ZONE);
        ZonedDateTime next = cron.next(from);
        assertThat(next).isNotNull();
        assertThat(next.getHour()).isEqualTo(9);
        assertThat(next.getMinute()).isEqualTo(30);
    }

    @Test
    void digestCron_firesAtSevenThirtyWeekdays() {
        CronExpression cron = CronExpression.parse("0 30 7 * * MON-FRI");
        ZonedDateTime from = ZonedDateTime.of(2026, 7, 13, 7, 0, 0, 0, ZONE);
        ZonedDateTime next = cron.next(from);
        assertThat(next).isEqualTo(ZonedDateTime.of(2026, 7, 13, 7, 30, 0, 0, ZONE));
    }
}
