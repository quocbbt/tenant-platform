package com.tenantcore.logiflowservice.application.reconciliation;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListEligibleCodQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListReconciliationsQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import com.tenantcore.logiflowservice.reconciliation.ReconciliationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReconciliationFacadeImpl implements ReconciliationFacade {

    private final ReconciliationService reconciliationService;

    public ReconciliationFacadeImpl(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Override
    public ReconciliationResponse createReconciliation(String tenantCode, CreateReconciliationRequest request) {
        return reconciliationService.createReconciliation(tenantCode, request);
    }

    @Override
    public PageResponse<EligibleCodRecordResponse> listEligibleCodRecords(String tenantCode, ListEligibleCodQuery query) {
        var page = reconciliationService.listEligibleCodRecords(tenantCode, query);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public PageResponse<ReconciliationResponse> listReconciliations(String tenantCode, ListReconciliationsQuery query) {
        var page = reconciliationService.listReconciliations(tenantCode, query);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public ReconciliationResponse getReconciliation(String tenantCode, UUID id) {
        return reconciliationService.getReconciliation(tenantCode, id);
    }

    @Override
    public ReconciliationResponse updateReconciliationStatus(String tenantCode, UUID id, UpdateReconciliationStatusRequest request) {
        return reconciliationService.updateReconciliationStatus(tenantCode, id, request);
    }
}
