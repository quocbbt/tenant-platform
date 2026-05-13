package com.tenantcore.logiflowservice.api.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateDriverRequest(
        @Schema(example = "DRV-001")
        @NotBlank String driverCode,
        @Schema(example = "Tran Van Tai")
        @NotBlank String fullName,
        @Schema(example = "0908111222")
        String phone,
        @Schema(example = "driver@example.com")
        String email,
        @Schema(example = "A1-123456789")
        String licenseNumber
) {
}
