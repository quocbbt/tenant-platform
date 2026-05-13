package com.tenantcore.logiflowservice.api.driver.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateDriverRequest(
        @Schema(example = "Tran Van Tai Updated")
        @NotBlank String fullName,
        @Schema(example = "0908333444")
        String phone,
        @Schema(example = "driver.updated@example.com")
        String email,
        @Schema(example = "A1-999999999")
        String licenseNumber,
        @Schema(example = "ACTIVE")
        String status
) {
}
