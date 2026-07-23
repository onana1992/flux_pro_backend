package com.nanotech.flux_pro_backend.service;

import com.nanotech.flux_pro_backend.common.AppException;
import com.nanotech.flux_pro_backend.dto.request.BusinessCalendarDayRequest;
import com.nanotech.flux_pro_backend.entity.BusinessCalendarDay;
import com.nanotech.flux_pro_backend.repository.BusinessCalendarDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessCalendarDayService {

    private static final String DEFAULT_COUNTRY = "CM";

    private final BusinessCalendarDayRepository repository;

    @Transactional(readOnly = true)
    public List<BusinessCalendarDay> list(Integer year, String countryCode) {
        String country = normalizeCountry(countryCode);
        if (year != null) {
            LocalDate from = LocalDate.of(year, 1, 1);
            LocalDate to = LocalDate.of(year, 12, 31);
            return repository.findByCountryCodeAndCalendarDateBetweenOrderByCalendarDateAsc(country, from, to);
        }
        return repository.findByCountryCodeOrderByCalendarDateAsc(country);
    }

    @Transactional(readOnly = true)
    public BusinessCalendarDay getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> AppException.notFound(
                        "BUSINESS_CALENDAR_NOT_FOUND", "Holiday not found"));
    }

    @Transactional
    public BusinessCalendarDay create(BusinessCalendarDayRequest request) {
        String country = normalizeCountry(request.countryCode());
        assertUnique(request.calendarDate(), country, null);
        BusinessCalendarDay day = new BusinessCalendarDay();
        apply(day, request, country);
        return repository.save(day);
    }

    @Transactional
    public BusinessCalendarDay update(UUID id, BusinessCalendarDayRequest request) {
        BusinessCalendarDay day = getById(id);
        String country = normalizeCountry(request.countryCode());
        assertUnique(request.calendarDate(), country, id);
        apply(day, request, country);
        return repository.save(day);
    }

    @Transactional
    public void delete(UUID id) {
        BusinessCalendarDay day = getById(id);
        repository.delete(day);
    }

    private void apply(BusinessCalendarDay day, BusinessCalendarDayRequest request, String country) {
        day.setCalendarDate(request.calendarDate());
        day.setLabel(request.label().trim());
        day.setCountryCode(country);
    }

    private void assertUnique(LocalDate date, String country, UUID excludeId) {
        repository.findByCalendarDateAndCountryCode(date, country).ifPresent(existing -> {
            if (excludeId == null || !existing.getId().equals(excludeId)) {
                throw AppException.conflict(
                        "BUSINESS_CALENDAR_DATE_EXISTS",
                        "A holiday already exists for this date: " + date,
                        date.toString());
            }
        });
    }

    private String normalizeCountry(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return DEFAULT_COUNTRY;
        }
        return countryCode.trim().toUpperCase();
    }
}
