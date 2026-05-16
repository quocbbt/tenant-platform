package com.tenantcore.coreservice.auth;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.coreservice.api.auth.dto.CurrentUserResponse;
import com.tenantcore.coreservice.api.auth.dto.LoginResponse;
import com.tenantcore.coreservice.api.auth.dto.TokenPairResponse;
import com.tenantcore.coreservice.domain.PermissionEntity;
import com.tenantcore.coreservice.domain.RefreshTokenEntity;
import com.tenantcore.coreservice.domain.RoleEntity;
import com.tenantcore.coreservice.domain.UserEntity;
import com.tenantcore.coreservice.repository.PermissionRepository;
import com.tenantcore.coreservice.repository.RefreshTokenRepository;
import com.tenantcore.coreservice.repository.RolePermissionRepository;
import com.tenantcore.coreservice.repository.RoleRepository;
import com.tenantcore.coreservice.repository.UserRepository;
import com.tenantcore.coreservice.repository.UserRoleRepository;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthProperties authProperties;

    public AuthService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            PermissionRepository permissionRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthProperties authProperties
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.permissionRepository = permissionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authProperties = authProperties;
    }

    public LoginResponse login(String tenantCode, String identifier, String rawPassword) {
        UserEntity user = userRepository
                .findByTenantCodeAndIdentifierAndStatusAndDeletedAtIsNull(tenantCode, identifier, SecurityConstants.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_USERNAME_OR_PASSWORD));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
        }

        List<String> roleCodes = resolveRoleCodes(tenantCode, user.getId());
        List<String> permissions = resolvePermissions(tenantCode, user.getId());

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                tenantCode,
                user.getUsername(),
                roleCodes,
                permissions
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId(), tenantCode);
        persistRefreshToken(user.getId(), tenantCode, refreshToken);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                authProperties.accessTokenExpirationSeconds(),
                new CurrentUserResponse(
                        user.getId(),
                        tenantCode,
                        user.getUsername(),
                        user.getFullName(),
                        roleCodes,
                        permissions
                )
        );
    }

    public CurrentUserResponse me(String bearerToken, String tenantCodeFromHeader) {
        String token = extractBearerToken(bearerToken);
        Claims claims = jwtService.parseToken(token);

        String tenantCodeInToken = claims.get(SecurityConstants.TENANT_CODE_CLAIM, String.class);
        if (tenantCodeFromHeader != null && !tenantCodeFromHeader.isBlank() && !tenantCodeFromHeader.equals(tenantCodeInToken)) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }

        UUID userId = UUID.fromString(claims.get(SecurityConstants.USER_ID_CLAIM, String.class));
        UserEntity user = userRepository
                .findByIdAndTenantCodeAndStatusAndDeletedAtIsNull(userId, tenantCodeInToken, SecurityConstants.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roleCodes = resolveRoleCodes(tenantCodeInToken, user.getId());
        List<String> permissions = resolvePermissions(tenantCodeInToken, user.getId());

        return new CurrentUserResponse(
                user.getId(),
                tenantCodeInToken,
                user.getUsername(),
                user.getFullName(),
                roleCodes,
                permissions
        );
    }

    public TokenPairResponse refresh(String tenantCodeFromHeader, String refreshToken) {
        Claims claims = jwtService.parseToken(refreshToken);
        String tenantCodeInToken = claims.get(SecurityConstants.TENANT_CODE_CLAIM, String.class);

        if (tenantCodeFromHeader != null && !tenantCodeFromHeader.isBlank() && !tenantCodeFromHeader.equals(tenantCodeInToken)) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }

        String tokenHash = sha256Hex(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        if (!SecurityConstants.STATUS_ACTIVE.equals(storedToken.getStatus())) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        if (storedToken.getRevokedAt() != null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        if (storedToken.getExpiredAt() != null && storedToken.getExpiredAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
        }

        UserEntity user = userRepository
                .findByIdAndTenantCodeAndStatusAndDeletedAtIsNull(storedToken.getUserId(), tenantCodeInToken, SecurityConstants.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<String> roleCodes = resolveRoleCodes(tenantCodeInToken, user.getId());
        List<String> permissions = resolvePermissions(tenantCodeInToken, user.getId());
        String newAccessToken = jwtService.generateAccessToken(
                user.getId(),
                tenantCodeInToken,
                user.getUsername(),
                roleCodes,
                permissions
        );

        return new TokenPairResponse(
                newAccessToken,
                refreshToken,
                "Bearer",
                authProperties.accessTokenExpirationSeconds()
        );
    }

    public void logout(String tenantCodeFromHeader, String refreshToken) {
        Claims claims = jwtService.parseToken(refreshToken);
        String tenantCodeInToken = claims.get(SecurityConstants.TENANT_CODE_CLAIM, String.class);
        if (tenantCodeFromHeader != null && !tenantCodeFromHeader.isBlank() && !tenantCodeFromHeader.equals(tenantCodeInToken)) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }

        String tokenHash = sha256Hex(refreshToken);
        RefreshTokenEntity storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        storedToken.setStatus(SecurityConstants.STATUS_INACTIVE);
        storedToken.setRevokedAt(LocalDateTime.now(ZoneOffset.UTC));
        refreshTokenRepository.save(storedToken);
    }

    public void setupDemoPassword(String tenantCode, String username, String rawPassword) {
        UserEntity user = userRepository
                .findByTenantCodeAndUsernameAndStatusAndDeletedAtIsNull(tenantCode, username, SecurityConstants.STATUS_ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!authorizationHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String token = authorizationHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return token;
    }

    private List<String> resolveRoleCodes(String tenantCode, UUID userId) {
        List<UUID> roleIds = userRoleRepository.findRoleIdsByTenantCodeAndUserId(tenantCode, userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<RoleEntity> roles = roleRepository.findByIdInAndTenantCodeAndStatus(roleIds, tenantCode, SecurityConstants.STATUS_ACTIVE);
        return roles.stream().map(RoleEntity::getRoleCode).distinct().toList();
    }

    private List<String> resolvePermissions(String tenantCode, UUID userId) {
        List<UUID> roleIds = userRoleRepository.findRoleIdsByTenantCodeAndUserId(tenantCode, userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<UUID> permissionIds = rolePermissionRepository.findPermissionIdsByRoleIds(roleIds);
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        List<PermissionEntity> permissions = permissionRepository.findByIdInAndStatus(permissionIds, SecurityConstants.STATUS_ACTIVE);
        Set<String> dedup = new LinkedHashSet<>();
        for (PermissionEntity permission : permissions) {
            dedup.add(permission.getPermissionCode());
        }
        return new ArrayList<>(dedup);
    }

    private void persistRefreshToken(UUID userId, String tenantCode, String refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setUserId(userId);
        entity.setTokenHash(sha256Hex(refreshToken));
        entity.setExpiredAt(LocalDateTime.now(ZoneOffset.UTC).plusSeconds(authProperties.refreshTokenExpirationSeconds()));
        entity.setStatus(SecurityConstants.STATUS_ACTIVE);
        refreshTokenRepository.save(entity);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
