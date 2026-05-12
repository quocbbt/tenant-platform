package com.tenantcore.logiflowservice.operations;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.operations.dto.CodDailyReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.ReconciliationDriverReportItem;
import com.tenantcore.logiflowservice.api.operations.dto.CodSummaryResponse;
import com.tenantcore.logiflowservice.order.CodRecordRepository;
import com.tenantcore.logiflowservice.reconciliation.ReconciliationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OperationsService {

    private final CodRecordRepository codRecordRepository;
    private final ReconciliationRepository reconciliationRepository;

    public OperationsService(
            CodRecordRepository codRecordRepository,
            ReconciliationRepository reconciliationRepository
    ) {
        this.codRecordRepository = codRecordRepository;
        this.reconciliationRepository = reconciliationRepository;
    }

    public CodSummaryResponse getCodSummary(String tenantCode) {
        var row = codRecordRepository.summarizeByTenant(tenantCode);
        if (row == null) {
            return new CodSummaryResponse(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new CodSummaryResponse(
                row.getTotalRecords(),
                row.getTotalAmount(),
                row.getPendingAmount(),
                row.getCollectedAmount(),
                row.getReconciledAmount()
        );
    }

    public List<CodDailyReportItem> getCodDailyReport(String tenantCode, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate == null ? LocalDate.now().minusDays(6) : fromDate;
        LocalDate end = toDate == null ? LocalDate.now() : toDate;
        validateDateRange(start, end);

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();
        return codRecordRepository.summarizeDailyByTenant(tenantCode, from, toExclusive)
                .stream()
                .map(p -> new CodDailyReportItem(
                        p.getBusinessDate(),
                        p.getTotalRecords(),
                        p.getTotalAmount(),
                        p.getPendingAmount(),
                        p.getCollectedAmount(),
                        p.getReconciledAmount()
                ))
                .toList();
    }

    public List<ReconciliationDriverReportItem> getReconciliationByDriver(String tenantCode, LocalDate fromDate, LocalDate toDate) {
        LocalDate start = fromDate == null ? LocalDate.now().minusDays(6) : fromDate;
        LocalDate end = toDate == null ? LocalDate.now() : toDate;
        validateDateRange(start, end);

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();
        return reconciliationRepository.summarizeByDriver(tenantCode, from, toExclusive)
                .stream()
                .map(p -> new ReconciliationDriverReportItem(
                        p.getDriverId(),
                        p.getTotalReconciliations(),
                        p.getTotalOrders(),
                        p.getTotalCodAmount(),
                        p.getReconciledCount()
                ))
                .toList();
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (toDate.isBefore(fromDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "toDate must be greater than or equal to fromDate");
        }
        if (fromDate.plusDays(31).isBefore(toDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Date range must not exceed 31 days");
        }
    }
}
