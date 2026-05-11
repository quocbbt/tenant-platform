package com.tenantcore.coreservice.api.auth.dto;

import java.util.List;
import java.util.UUID;

public record CurrentUserResponse(
        UUID userId,
        String tenantCode,
        String username,
        String fullName,
        List<String> roles,
        List<String> permissions
) {
}
