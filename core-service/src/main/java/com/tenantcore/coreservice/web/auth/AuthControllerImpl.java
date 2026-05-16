package com.tenantcore.coreservice.web.auth;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.coreservice.api.auth.AuthApi;
import com.tenantcore.coreservice.api.auth.dto.CurrentUserResponse;
import com.tenantcore.coreservice.api.auth.dto.LoginRequest;
import com.tenantcore.coreservice.api.auth.dto.LoginResponse;
import com.tenantcore.coreservice.api.auth.dto.RefreshRequest;
import com.tenantcore.coreservice.api.auth.dto.SetupPasswordRequest;
import com.tenantcore.coreservice.api.auth.dto.TokenPairResponse;
import com.tenantcore.coreservice.application.auth.AuthFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class AuthControllerImpl implements AuthApi {

    private final AuthFacade authFacade;

    public AuthControllerImpl(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @Override
    @PostMapping("/api/auth/login")
    public ApiResponse<LoginResponse> login(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody LoginRequest request
    ) {
        String effectiveTenant = tenantCode == null || tenantCode.isBlank() ? "demo" : tenantCode;
        LoginResponse response = authFacade.login(effectiveTenant, request.identifier(), request.password());
        return ApiResponse.success("Login success", response);
    }

    @Override
    @GetMapping("/api/auth/me")
    public ApiResponse<CurrentUserResponse> me(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        CurrentUserResponse response = authFacade.me(authorizationHeader, tenantCode);
        return ApiResponse.success(response);
    }

    @Override
    @PostMapping("/api/auth/setup-password")
    public ApiResponse<String> setupPassword(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody SetupPasswordRequest request
    ) {
        String effectiveTenant = tenantCode == null || tenantCode.isBlank() ? "demo" : tenantCode;
        authFacade.setupPassword(effectiveTenant, request.username(), request.password());
        return ApiResponse.success("Setup password success", "OK");
    }

    @Override
    @PostMapping("/api/auth/refresh")
    public ApiResponse<TokenPairResponse> refresh(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody RefreshRequest request
    ) {
        TokenPairResponse response = authFacade.refresh(tenantCode, request.refreshToken());
        return ApiResponse.success("Refresh token success", response);
    }

    @Override
    @PostMapping("/api/auth/logout")
    public ApiResponse<String> logout(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody RefreshRequest request
    ) {
        authFacade.logout(tenantCode, request.refreshToken());
        return ApiResponse.success("Logout success", "OK");
    }
}
