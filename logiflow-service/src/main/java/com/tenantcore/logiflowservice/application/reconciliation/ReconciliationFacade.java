package com.tenantcore.logiflowservice.application.reconciliation;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListEligibleCodQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListReconciliationsQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;

import java.util.UUID;

public interface ReconciliationFacade {

    ReconciliationResponse createReconciliation(String tenantCode, CreateReconciliationRequest request);

    PageResponse<EligibleCodRecordResponse> listEligibleCodRecords(String tenantCode, ListEligibleCodQuery query);

    PageResponse<ReconciliationResponse> listReconciliations(String tenantCode, ListReconciliationsQuery query);

    ReconciliationResponse getReconciliation(String tenantCode, UUID id);

    ReconciliationResponse updateReconciliationStatus(String tenantCode, UUID id, UpdateReconciliationStatusRequest request);
}
