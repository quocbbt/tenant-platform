package com.tenantcore.coreservice.application.auth;

import com.tenantcore.coreservice.api.auth.dto.CurrentUserResponse;
import com.tenantcore.coreservice.api.auth.dto.LoginResponse;
import com.tenantcore.coreservice.api.auth.dto.TokenPairResponse;
import com.tenantcore.coreservice.auth.AuthService;
import org.springframework.stereotype.Component;

@Component
public class AuthFacadeImpl implements AuthFacade {

    private final AuthService authService;

    public AuthFacadeImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public LoginResponse login(String tenantCode, String identifier, String password) {
        return authService.login(tenantCode, identifier, password);
    }

    @Override
    public CurrentUserResponse me(String authorizationHeader, String tenantCode) {
        return authService.me(authorizationHeader, tenantCode);
    }

    @Override
    public void setupPassword(String tenantCode, String username, String password) {
        authService.setupDemoPassword(tenantCode, username, password);
    }

    @Override
    public TokenPairResponse refresh(String tenantCode, String refreshToken) {
        return authService.refresh(tenantCode, refreshToken);
    }

    @Override
    public void logout(String tenantCode, String refreshToken) {
        authService.logout(tenantCode, refreshToken);
    }
}
