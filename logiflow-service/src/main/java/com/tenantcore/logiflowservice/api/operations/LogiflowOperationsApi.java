package com.tenantcore.logiflowservice.api.operations;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.logiflowservice.api.operations.dto.CodDailyReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.api.operations.dto.ReconciliationDriverReportItem;

import java.time.LocalDate;
import java.util.List;

public interface LogiflowOperationsApi {

    ApiResponse<CodSummaryResponse> getCodSummary(String tenantCode);

    ApiResponse<List<CodDailyReportItem>> getCodDailyReport(String tenantCode, LocalDate fromDate, LocalDate toDate);

    ApiResponse<List<ReconciliationDriverReportItem>> getReconciliationByDriver(String tenantCode, LocalDate fromDate, LocalDate toDate);
}
