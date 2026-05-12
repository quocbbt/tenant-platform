package com.tenantcore.logiflowservice.api.operations.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconciliationDriverReportItem(
        UUID driverId,
        long totalReconciliations,
        long totalOrders,
        BigDecimal totalCodAmount,
        long reconciledCount
) {
}
