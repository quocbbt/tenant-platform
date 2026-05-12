package com.tenantcore.logiflowservice.api.driver.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDriverRequest(
        @NotBlank String driverCode,
        @NotBlank String fullName,
        String phone,
        String email,
        String licenseNumber
) {
}
