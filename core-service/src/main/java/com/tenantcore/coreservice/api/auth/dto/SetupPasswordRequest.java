package com.tenantcore.coreservice.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupPasswordRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
