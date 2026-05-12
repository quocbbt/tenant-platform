package com.tenantcore.logiflowservice.api.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleResponse(
        UUID id,
        String tenantCode,
        String vehicleCode,
        String plateNumber,
        String vehicleType,
        BigDecimal capacityKg,
        String status,
        LocalDateTime createdAt
) {
}
