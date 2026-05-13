package com.tenantcore.logiflowservice.api.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateVehicleRequest(
        @Schema(example = "51A-67890")
        @NotBlank String plateNumber,
        @Schema(example = "TRUCK")
        String vehicleType,
        @Schema(example = "2500")
        BigDecimal capacityKg,
        @Schema(example = "ACTIVE")
        String status
) {
}
