package com.tenantcore.logiflowservice.api.reconciliation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EligibleCodRecordResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {
}
