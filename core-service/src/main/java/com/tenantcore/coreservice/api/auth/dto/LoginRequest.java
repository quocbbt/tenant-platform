package com.tenantcore.coreservice.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Username, email, or phone is required") String identifier,
        @NotBlank String password
) {
}
