package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.enumeration.DelayUnit;
import com.nanotech.flux_pro_backend.repository.BusinessCalendarDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DelaiService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Douala");
    private static final LocalTime WORK_START = LocalTime.of(8, 0);
    private static final LocalTime WORK_END = LocalTime.of(17, 0);
    private static final String COUNTRY_CODE = "CM";

    private final BusinessCalendarDayRepository businessCalendarDayRepository;

    /**
     * Calcule l'échéance. Un délai ≤ 0 (ex. étape de clôture) signifie « pas d'échéance » → {@code null}.
     */
    public Instant calculateDueDate(Instant start, int delayValue, DelayUnit unit) {
        if (delayValue <= 0) {
            return null;
        }
        ZonedDateTime zonedStart = ZonedDateTime.ofInstant(start, BUSINESS_ZONE);
        if (unit == DelayUnit.WORKING_HOURS) {
            return addWorkingHours(zonedStart, delayValue).toInstant();
        }
        return endOfWorkingDay(addWorkingDays(zonedStart, delayValue)).toInstant();
    }

    public boolean isOverdue(Instant dueAt, Instant now) {
        return now.isAfter(dueAt);
    }

    /**
     * Décale une échéance d'un nombre de jours/heures ouvrés positif ou négatif (ALR-06).
     * Utilisé par le moteur d'alertes pour calculer un seuil (ex. J-2, J+3) par rapport à
     * {@code FilePassage.dueAt} — jamais par rapport à la date de réception.
     */
    public Instant applyOffset(Instant dueAt, int offsetValue, DelayUnit unit) {
        if (offsetValue == 0) {
            return dueAt;
        }
        ZonedDateTime zonedDueAt = ZonedDateTime.ofInstant(dueAt, BUSINESS_ZONE);
        if (offsetValue > 0) {
            return unit == DelayUnit.WORKING_HOURS
                    ? addWorkingHours(zonedDueAt, offsetValue).toInstant()
                    : endOfWorkingDay(addWorkingDays(zonedDueAt, offsetValue)).toInstant();
        }
        return unit == DelayUnit.WORKING_HOURS
                ? subtractWorkingHours(zonedDueAt, -offsetValue).toInstant()
                : subtractWorkingDays(zonedDueAt, -offsetValue).toInstant();
    }

    public int countWorkingDays(Instant start, Instant end) {
        if (end.isBefore(start)) {
            return 0;
        }
        ZonedDateTime from = ZonedDateTime.ofInstant(start, BUSINESS_ZONE).truncatedTo(ChronoUnit.DAYS);
        ZonedDateTime to = ZonedDateTime.ofInstant(end, BUSINESS_ZONE).truncatedTo(ChronoUnit.DAYS);
        Set<LocalDate> holidays = loadHolidays(from.toLocalDate(), to.toLocalDate());
        int days = 0;
        for (LocalDate date = from.toLocalDate(); !date.isAfter(to.toLocalDate()); date = date.plusDays(1)) {
            if (isWorkingDay(date, holidays)) {
                days++;
            }
        }
        return Math.max(days, 1);
    }

    public BigDecimal calculateConsumedHours(Instant start, Instant end, Instant suspendedAt, Instant resumedAt) {
        if (end.isBefore(start)) {
            return BigDecimal.ZERO;
        }
        double hours = calculateWorkingHoursBetween(start, end);
        if (suspendedAt != null && resumedAt != null && resumedAt.isAfter(suspendedAt)) {
            hours -= calculateWorkingHoursBetween(suspendedAt, resumedAt);
        }
        return BigDecimal.valueOf(Math.max(hours, 0)).setScale(2, RoundingMode.HALF_UP);
    }

    public ZonedDateTime addWorkingDays(ZonedDateTime start, int workingDays) {
        ZonedDateTime cursor = alignToWorkingTime(start);
        Set<LocalDate> holidays = loadHolidays(cursor.toLocalDate(), cursor.toLocalDate().plusDays(workingDays + 14L));
        int added = 0;
        while (added < workingDays) {
            cursor = cursor.plusDays(1).with(WORK_START);
            if (isWorkingDay(cursor.toLocalDate(), holidays)) {
                added++;
            }
        }
        return cursor;
    }

    public ZonedDateTime addWorkingHours(ZonedDateTime start, int workingHours) {
        ZonedDateTime cursor = alignToWorkingTime(start);
        int remainingMinutes = workingHours * 60;
        Set<LocalDate> holidays = loadHolidays(
                cursor.toLocalDate(), cursor.toLocalDate().plusDays(workingHours / 8L + 14L));

        while (remainingMinutes > 0) {
            if (!isWorkingDay(cursor.toLocalDate(), holidays)) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }
            ZonedDateTime dayEnd = cursor.with(WORK_END);
            long minutesLeftToday = ChronoUnit.MINUTES.between(cursor, dayEnd);
            if (minutesLeftToday <= 0) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }
            int consume = (int) Math.min(minutesLeftToday, remainingMinutes);
            cursor = cursor.plusMinutes(consume);
            remainingMinutes -= consume;
            if (remainingMinutes > 0) {
                cursor = cursor.plusDays(1).with(WORK_START);
            }
        }
        return cursor;
    }

    public ZonedDateTime subtractWorkingDays(ZonedDateTime start, int workingDays) {
        ZonedDateTime cursor = alignToWorkingTime(start);
        Set<LocalDate> holidays = loadHolidays(cursor.toLocalDate().minusDays(workingDays + 14L), cursor.toLocalDate());
        int removed = 0;
        while (removed < workingDays) {
            cursor = cursor.minusDays(1).with(WORK_START);
            if (isWorkingDay(cursor.toLocalDate(), holidays)) {
                removed++;
            }
        }
        return cursor;
    }

    public ZonedDateTime subtractWorkingHours(ZonedDateTime start, int workingHours) {
        ZonedDateTime cursor = alignToWorkingTime(start);
        int remainingMinutes = workingHours * 60;
        Set<LocalDate> holidays = loadHolidays(
                cursor.toLocalDate().minusDays(workingHours / 8L + 14L), cursor.toLocalDate());

        while (remainingMinutes > 0) {
            if (!isWorkingDay(cursor.toLocalDate(), holidays)) {
                cursor = cursor.minusDays(1).with(WORK_END);
                continue;
            }
            long minutesAvailableToday = ChronoUnit.MINUTES.between(cursor.with(WORK_START), cursor);
            if (minutesAvailableToday <= 0) {
                cursor = cursor.minusDays(1).with(WORK_END);
                continue;
            }
            int consume = (int) Math.min(minutesAvailableToday, remainingMinutes);
            cursor = cursor.minusMinutes(consume);
            remainingMinutes -= consume;
            if (remainingMinutes > 0) {
                cursor = cursor.minusDays(1).with(WORK_END);
            }
        }
        return cursor;
    }

    private ZonedDateTime endOfWorkingDay(ZonedDateTime dateTime) {
        ZonedDateTime aligned = alignToWorkingTime(dateTime);
        return aligned.with(WORK_END);
    }

    private ZonedDateTime alignToWorkingTime(ZonedDateTime dateTime) {
        ZonedDateTime cursor = dateTime.withZoneSameInstant(BUSINESS_ZONE);
        Set<LocalDate> holidays = loadHolidays(cursor.toLocalDate(), cursor.toLocalDate().plusDays(7));
        while (!isWorkingDay(cursor.toLocalDate(), holidays)) {
            cursor = cursor.plusDays(1).with(WORK_START);
            holidays = loadHolidays(cursor.toLocalDate(), cursor.toLocalDate().plusDays(7));
        }
        if (cursor.toLocalTime().isBefore(WORK_START)) {
            return cursor.with(WORK_START);
        }
        if (!cursor.toLocalTime().isBefore(WORK_END)) {
            ZonedDateTime next = cursor.plusDays(1).with(WORK_START);
            holidays = loadHolidays(next.toLocalDate(), next.toLocalDate().plusDays(7));
            while (!isWorkingDay(next.toLocalDate(), holidays)) {
                next = next.plusDays(1).with(WORK_START);
            }
            return next;
        }
        return cursor;
    }

    private double calculateWorkingHoursBetween(Instant start, Instant end) {
        ZonedDateTime cursor = alignToWorkingTime(ZonedDateTime.ofInstant(start, BUSINESS_ZONE));
        ZonedDateTime endZoned = ZonedDateTime.ofInstant(end, BUSINESS_ZONE);
        if (!endZoned.isAfter(cursor)) {
            return 0;
        }
        double totalMinutes = 0;
        Set<LocalDate> holidays = loadHolidays(cursor.toLocalDate(), endZoned.toLocalDate().plusDays(1));
        while (cursor.isBefore(endZoned)) {
            if (!isWorkingDay(cursor.toLocalDate(), holidays)) {
                cursor = cursor.plusDays(1).with(WORK_START);
                continue;
            }
            ZonedDateTime dayEnd = cursor.with(WORK_END);
            ZonedDateTime segmentEnd = endZoned.isBefore(dayEnd) ? endZoned : dayEnd;
            if (segmentEnd.isAfter(cursor)) {
                totalMinutes += ChronoUnit.MINUTES.between(cursor, segmentEnd);
            }
            cursor = cursor.plusDays(1).with(WORK_START);
        }
        return totalMinutes / 60.0;
    }

    private boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidays.contains(date);
    }

    private Set<LocalDate> loadHolidays(LocalDate from, LocalDate to) {
        Set<LocalDate> holidays = new HashSet<>(
                businessCalendarDayRepository.findHolidayDatesBetween(COUNTRY_CODE, from, to));
        for (int year = from.getYear(); year <= to.getYear(); year++) {
            holidays.add(LocalDate.of(year, 1, 1));
            holidays.add(LocalDate.of(year, 2, 11));
            holidays.add(LocalDate.of(year, 5, 1));
            holidays.add(LocalDate.of(year, 5, 20));
            holidays.add(LocalDate.of(year, 12, 25));
        }
        return holidays;
    }
}
