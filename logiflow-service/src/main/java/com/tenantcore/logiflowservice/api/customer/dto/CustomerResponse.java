package com.tenantcore.logiflowservice.api.customer.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String tenantCode,
        String customerCode,
        String customerName,
        String phone,
        String email,
        String address,
        String type,
        String status,
        LocalDateTime createdAt
) {
}
