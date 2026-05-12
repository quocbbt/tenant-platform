package com.tenantcore.logiflowservice.api.operations.dto;

import java.math.BigDecimal;

public record CodSummaryResponse(
        long totalRecords,
        BigDecimal totalAmount,
        BigDecimal pendingAmount,
        BigDecimal collectedAmount,
        BigDecimal reconciledAmount
) {
}
