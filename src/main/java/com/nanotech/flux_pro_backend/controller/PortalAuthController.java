package com.nanotech.flux_pro_backend.controller;

import com.nanotech.flux_pro_backend.dto.request.PortalActivateRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalLoginRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalOtpRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalOtpVerifyRequest;
import com.nanotech.flux_pro_backend.dto.request.PortalRegisterRequest;
import com.nanotech.flux_pro_backend.dto.response.PortalAuthResponse;
import com.nanotech.flux_pro_backend.dto.response.PortalOtpSentResponse;
import com.nanotech.flux_pro_backend.service.portal.PortalAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portal/auth")
@RequiredArgsConstructor
public class PortalAuthController {

    private final PortalAuthService portalAuthService;

    @PostMapping("/login")
    public PortalAuthResponse login(
            @Valid @RequestBody PortalLoginRequest request, HttpServletRequest httpRequest) {
        return portalAuthService.login(request, httpRequest);
    }

    @PostMapping("/activate")
    public PortalAuthResponse activate(
            @Valid @RequestBody PortalActivateRequest request, HttpServletRequest httpRequest) {
        return portalAuthService.activate(request, httpRequest);
    }

    @PostMapping("/register")
    public PortalOtpSentResponse register(
            @Valid @RequestBody PortalRegisterRequest request, HttpServletRequest httpRequest) {
        return portalAuthService.register(request, httpRequest);
    }

    @PostMapping("/otp/request")
    public PortalOtpSentResponse requestOtp(
            @Valid @RequestBody PortalOtpRequest request, HttpServletRequest httpRequest) {
        return portalAuthService.requestOtp(request, httpRequest);
    }

    @PostMapping("/otp/verify")
    public PortalAuthResponse verifyOtp(
            @Valid @RequestBody PortalOtpVerifyRequest request, HttpServletRequest httpRequest) {
        return portalAuthService.verifyOtp(request, httpRequest);
    }
}
