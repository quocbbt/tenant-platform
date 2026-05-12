package com.tenantcore.logiflowservice.order;

import java.math.BigDecimal;

public interface CodSummaryProjection {

    long getTotalRecords();

    BigDecimal getTotalAmount();

    BigDecimal getPendingAmount();

    BigDecimal getCollectedAmount();

    BigDecimal getReconciledAmount();
}
