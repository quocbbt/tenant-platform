package com.tenantcore.logiflowservice.api.operations.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CodDailyReportItem(
        LocalDate businessDate,
        long totalRecords,
        BigDecimal totalAmount,
        BigDecimal pendingAmount,
        BigDecimal collectedAmount,
        BigDecimal reconciledAmount
) {
}
