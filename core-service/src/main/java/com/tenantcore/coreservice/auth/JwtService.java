package com.tenantcore.coreservice.auth;

import com.tenantcore.common.security.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {

    private final AuthProperties authProperties;
    private final SecretKey secretKey;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.secretKey = Keys.hmacShaKeyFor(authProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(
            UUID userId,
            String tenantCode,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(authProperties.accessTokenExpirationSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(authProperties.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(SecurityConstants.USER_ID_CLAIM, userId.toString())
                .claim(SecurityConstants.TENANT_CODE_CLAIM, tenantCode)
                .claim(SecurityConstants.USERNAME_CLAIM, username)
                .claim(SecurityConstants.ROLES_CLAIM, roles)
                .claim(SecurityConstants.PERMISSIONS_CLAIM, permissions)
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(UUID userId, String tenantCode) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(authProperties.refreshTokenExpirationSeconds());

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(authProperties.issuer())
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(SecurityConstants.USER_ID_CLAIM, userId.toString())
                .claim(SecurityConstants.TENANT_CODE_CLAIM, tenantCode)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
