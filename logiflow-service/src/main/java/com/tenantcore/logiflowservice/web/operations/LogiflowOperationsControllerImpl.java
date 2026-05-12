package com.tenantcore.logiflowservice.web.operations;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.operations.LogiflowOperationsApi;
import com.tenantcore.logiflowservice.api.operations.dto.CodDailyReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.api.operations.dto.ReconciliationDriverReportItem;
import com.tenantcore.logiflowservice.application.operations.OperationsFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "Operations", description = "Operational reports for COD and reconciliation")
public class LogiflowOperationsControllerImpl implements LogiflowOperationsApi {

    private final OperationsFacade operationsFacade;

    public LogiflowOperationsControllerImpl(OperationsFacade operationsFacade) {
        this.operationsFacade = operationsFacade;
    }

    @Override
    @Operation(summary = "Get COD summary", description = "Return COD total/pending/collected/reconciled amounts for current tenant")
    @GetMapping("/api/logiflow/operations/cod/summary")
    public ApiResponse<CodSummaryResponse> getCodSummary(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        CodSummaryResponse response = operationsFacade.getCodSummary(resolveTenantCode(tenantCode));
        return ApiResponse.success(response);
    }

    @Override
    @Operation(summary = "Get COD daily report", description = "Return daily COD aggregates in a date range (default last 7 days, max 31 days)")
    @GetMapping("/api/logiflow/operations/cod/daily")
    public ApiResponse<List<CodDailyReportItem>> getCodDailyReport(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate
    ) {
        List<CodDailyReportItem> response = operationsFacade.getCodDailyReport(resolveTenantCode(tenantCode), fromDate, toDate);
        return ApiResponse.success(response);
    }

    @Override
    @Operation(summary = "Get reconciliation by driver report", description = "Aggregate reconciliation totals by driver in a date range (default last 7 days, max 31 days)")
    @GetMapping("/api/logiflow/operations/reconciliation/by-driver")
    public ApiResponse<List<ReconciliationDriverReportItem>> getReconciliationByDriver(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) LocalDate toDate
    ) {
        List<ReconciliationDriverReportItem> response =
                operationsFacade.getReconciliationByDriver(resolveTenantCode(tenantCode), fromDate, toDate);
        return ApiResponse.success(response);
    }

    private String resolveTenantCode(String tenantCodeHeader) {
        var currentUser = com.tenantcore.common.context.UserContext.getCurrentUser();
        if (currentUser == null || currentUser.tenantCode() == null || currentUser.tenantCode().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (tenantCodeHeader == null || tenantCodeHeader.isBlank()) {
            return currentUser.tenantCode();
        }
        if (!tenantCodeHeader.equals(currentUser.tenantCode())) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }
        return tenantCodeHeader;
    }
}
