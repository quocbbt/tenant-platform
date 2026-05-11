package com.tenantcore.coreservice.api.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        CurrentUserResponse user
) {
}
