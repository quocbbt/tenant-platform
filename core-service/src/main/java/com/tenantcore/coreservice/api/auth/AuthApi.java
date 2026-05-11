package com.tenantcore.coreservice.api.auth;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.coreservice.api.auth.dto.CurrentUserResponse;
import com.tenantcore.coreservice.api.auth.dto.LoginRequest;
import com.tenantcore.coreservice.api.auth.dto.LoginResponse;
import com.tenantcore.coreservice.api.auth.dto.RefreshRequest;
import com.tenantcore.coreservice.api.auth.dto.SetupPasswordRequest;
import com.tenantcore.coreservice.api.auth.dto.TokenPairResponse;
import jakarta.validation.Valid;

public interface AuthApi {

    ApiResponse<LoginResponse> login(String tenantCode, @Valid LoginRequest request);

    ApiResponse<CurrentUserResponse> me(String authorizationHeader, String tenantCode);

    ApiResponse<String> setupPassword(String tenantCode, @Valid SetupPasswordRequest request);

    ApiResponse<TokenPairResponse> refresh(String tenantCode, @Valid RefreshRequest request);

    ApiResponse<String> logout(String tenantCode, @Valid RefreshRequest request);
}
