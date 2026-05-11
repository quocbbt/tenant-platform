package com.tenantcore.coreservice.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.jwt")
public record AuthProperties(
        String issuer,
        String secret,
        long accessTokenExpirationSeconds,
        long refreshTokenExpirationSeconds
) {
}
