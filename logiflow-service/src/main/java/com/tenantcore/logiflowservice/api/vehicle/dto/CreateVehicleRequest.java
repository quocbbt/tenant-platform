package com.tenantcore.logiflowservice.api.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record CreateVehicleRequest(
        @Schema(example = "VEH-001")
        @NotBlank String vehicleCode,
        @Schema(example = "51A-12345")
        @NotBlank String plateNumber,
        @Schema(example = "VAN")
        String vehicleType,
        @Schema(example = "1200")
        BigDecimal capacityKg
) {
}
