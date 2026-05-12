package com.tenantcore.logiflowservice.application.operations;

import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.api.operations.dto.CodDailyReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.ReconciliationDriverReportItem;
import com.tenantcore.logiflowservice.operations.OperationsService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class OperationsFacadeImpl implements OperationsFacade {

    private final OperationsService operationsService;

    public OperationsFacadeImpl(OperationsService operationsService) {
        this.operationsService = operationsService;
    }

    @Override
    public CodSummaryResponse getCodSummary(String tenantCode) {
        return operationsService.getCodSummary(tenantCode);
    }

    @Override
    public List<CodDailyReportItem> getCodDailyReport(String tenantCode, LocalDate fromDate, LocalDate toDate) {
        return operationsService.getCodDailyReport(tenantCode, fromDate, toDate);
    }

    @Override
    public List<ReconciliationDriverReportItem> getReconciliationByDriver(String tenantCode, LocalDate fromDate, LocalDate toDate) {
        return operationsService.getReconciliationByDriver(tenantCode, fromDate, toDate);
    }
}
