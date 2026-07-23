package com.nanotech.flux_pro_backend.mapper;

import com.nanotech.flux_pro_backend.dto.response.BusinessCalendarDayResponse;
import com.nanotech.flux_pro_backend.entity.BusinessCalendarDay;

public final class BusinessCalendarDayMapper {

    private BusinessCalendarDayMapper() {
    }

    public static BusinessCalendarDayResponse toResponse(BusinessCalendarDay day) {
        return new BusinessCalendarDayResponse(
                day.getId(),
                day.getCalendarDate(),
                day.getLabel(),
                day.getCountryCode());
    }
}
