package com.tenantcore.coreservice.api.auth.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
