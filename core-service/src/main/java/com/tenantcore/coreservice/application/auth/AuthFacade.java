package com.tenantcore.coreservice.application.auth;

import com.tenantcore.coreservice.api.auth.dto.CurrentUserResponse;
import com.tenantcore.coreservice.api.auth.dto.LoginResponse;
import com.tenantcore.coreservice.api.auth.dto.TokenPairResponse;

public interface AuthFacade {

    LoginResponse login(String tenantCode, String identifier, String password);

    CurrentUserResponse me(String authorizationHeader, String tenantCode);

    void setupPassword(String tenantCode, String username, String password);

    TokenPairResponse refresh(String tenantCode, String refreshToken);

    void logout(String tenantCode, String refreshToken);
}
