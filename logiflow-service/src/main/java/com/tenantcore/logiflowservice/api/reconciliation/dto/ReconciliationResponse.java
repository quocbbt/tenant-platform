package com.tenantcore.logiflowservice.api.reconciliation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ReconciliationResponse(
        UUID id,
        String tenantCode,
        String reconciliationCode,
        UUID driverId,
        Integer totalOrders,
        BigDecimal totalCodAmount,
        String status,
        LocalDateTime reconciledAt,
        String note,
        LocalDateTime createdAt
) {
}
