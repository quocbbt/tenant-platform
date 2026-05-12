package com.tenantcore.logiflowservice.reconciliation;

import java.math.BigDecimal;
import java.util.UUID;

public interface ReconciliationDriverProjection {

    UUID getDriverId();

    long getTotalReconciliations();

    long getTotalOrders();

    BigDecimal getTotalCodAmount();

    long getReconciledCount();
}
