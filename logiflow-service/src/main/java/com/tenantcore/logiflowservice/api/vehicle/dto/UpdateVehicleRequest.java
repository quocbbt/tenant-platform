package com.tenantcore.logiflowservice.api.vehicle.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record UpdateVehicleRequest(
        @NotBlank String plateNumber,
        String vehicleType,
        BigDecimal capacityKg,
        String status
) {
}
