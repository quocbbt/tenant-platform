package com.tenantcore.logiflowservice.api.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String tenantCode,
        String orderCode,
        String receiverName,
        String receiverAddress,
        BigDecimal codAmount,
        String status,
        LocalDateTime createdAt
) {
}
