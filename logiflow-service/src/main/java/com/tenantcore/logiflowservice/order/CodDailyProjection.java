package com.tenantcore.logiflowservice.order;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface CodDailyProjection {

    LocalDate getBusinessDate();

    long getTotalRecords();

    BigDecimal getTotalAmount();

    BigDecimal getPendingAmount();

    BigDecimal getCollectedAmount();

    BigDecimal getReconciledAmount();
}
