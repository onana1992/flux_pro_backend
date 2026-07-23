package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.BusinessCalendarDayRequest;
import com.nanotech.flux_pro_backend.dto.response.BusinessCalendarDayResponse;
import com.nanotech.flux_pro_backend.mapper.BusinessCalendarDayMapper;
import com.nanotech.flux_pro_backend.security.RbacPermissions;
import com.nanotech.flux_pro_backend.security.RequiresPermission;
import com.nanotech.flux_pro_backend.service.BusinessCalendarDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/business-calendar")
@RequiredArgsConstructor
public class BusinessCalendarDayController {

    private final BusinessCalendarDayService service;

    @GetMapping
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_READ)
    public List<BusinessCalendarDayResponse> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String countryCode) {
        return service.list(year, countryCode).stream()
                .map(BusinessCalendarDayMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_READ)
    public BusinessCalendarDayResponse getById(@PathVariable UUID id) {
        return BusinessCalendarDayMapper.toResponse(service.getById(id));
    }

    @PostMapping
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessCalendarDayResponse create(@Valid @RequestBody BusinessCalendarDayRequest request) {
        return BusinessCalendarDayMapper.toResponse(service.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_UPDATE)
    public BusinessCalendarDayResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody BusinessCalendarDayRequest request) {
        return BusinessCalendarDayMapper.toResponse(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission(RbacPermissions.BUSINESS_CALENDAR_DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
