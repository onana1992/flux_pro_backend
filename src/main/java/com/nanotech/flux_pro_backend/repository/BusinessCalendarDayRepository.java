package com.nanotech.flux_pro_backend.repository;

import com.nanotech.flux_pro_backend.entity.BusinessCalendarDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface BusinessCalendarDayRepository extends JpaRepository<BusinessCalendarDay, UUID> {

    @Query("""
            SELECT b.calendarDate FROM BusinessCalendarDay b
            WHERE b.countryCode = :countryCode
              AND b.calendarDate BETWEEN :from AND :to
            """)
    List<LocalDate> findHolidayDatesBetween(
            @Param("countryCode") String countryCode,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
