package com.tenantcore.logiflowservice.api.reconciliation;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface LogiflowReconciliationApi {

    ApiResponse<ReconciliationResponse> createReconciliation(String tenantCode, @Valid CreateReconciliationRequest request);

    ApiResponse<PageResponse<EligibleCodRecordResponse>> listEligibleCodRecords(
            String tenantCode,
            int page,
            int size,
            String keyword
    );

    ApiResponse<PageResponse<ReconciliationResponse>> listReconciliations(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<ReconciliationResponse> getReconciliation(String tenantCode, UUID id);

    ApiResponse<ReconciliationResponse> updateReconciliationStatus(
            String tenantCode,
            UUID id,
            @Valid UpdateReconciliationStatusRequest request
    );
}
