package com.nanotech.flux_pro_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "business_calendar")
@Getter
@Setter
public class BusinessCalendarDay extends BaseEntity {

    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode = "CM";
}
