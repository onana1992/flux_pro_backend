package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.ClockAdjustRequest;
import com.nanotech.flux_pro_backend.dto.request.ClockSetRequest;
import com.nanotech.flux_pro_backend.dto.response.SystemClockResponse;
import com.nanotech.flux_pro_backend.security.SecurityUtils;
import com.nanotech.flux_pro_backend.service.AlertEngineService;
import com.nanotech.flux_pro_backend.service.ClockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system/clock")
@RequiredArgsConstructor
public class SystemClockController {

    private final ClockService clockService;
    private final AlertEngineService alertEngineService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public SystemClockResponse status() {
        return clockService.status();
    }

    @PostMapping("/adjust")
    public SystemClockResponse adjust(@Valid @RequestBody ClockAdjustRequest request) {
        return clockService.adjust(request, securityUtils.currentUser());
    }

    @PostMapping("/set")
    public SystemClockResponse set(@Valid @RequestBody ClockSetRequest request) {
        return clockService.set(request.instant(), securityUtils.currentUser());
    }

    @PostMapping("/reset")
    public SystemClockResponse reset() {
        return clockService.resetToWall(securityUtils.currentUser());
    }

    /** Déclenche immédiatement le moteur d'alertes (utile après un saut temporel en mode TEST). */
    @PostMapping("/run-alert-engine")
    public ResponseEntity<Map<String, Object>> runAlertEngine() {
        clockService.assertCanMutate(securityUtils.currentUser());
        alertEngineService.evaluateAll();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "now", clockService.now().toString(),
                "mode", clockService.getMode().name()));
    }
}
