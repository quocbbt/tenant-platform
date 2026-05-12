package com.tenantcore.logiflowservice.application.operations;

import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.api.operations.dto.CodDailyReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.ReconciliationDriverReportItem;

import java.time.LocalDate;
import java.util.List;

public interface OperationsFacade {

    CodSummaryResponse getCodSummary(String tenantCode);

    List<CodDailyReportItem> getCodDailyReport(String tenantCode, LocalDate fromDate, LocalDate toDate);

    List<ReconciliationDriverReportItem> getReconciliationByDriver(String tenantCode, LocalDate fromDate, LocalDate toDate);
}
