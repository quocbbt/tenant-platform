package com.tenantcore.logiflowservice.api.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDriverRequest(
        @NotBlank String fullName,
        String phone,
        String email,
        String licenseNumber,
        String status
) {
}
