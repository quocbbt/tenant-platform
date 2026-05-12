package com.tenantcore.logiflowservice.api.driver.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DriverResponse(
        UUID id,
        String tenantCode,
        String driverCode,
        String fullName,
        String phone,
        String email,
        String licenseNumber,
        String status,
        LocalDateTime createdAt
) {
}
