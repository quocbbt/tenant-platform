package com.tenantcore.logiflowservice.web.reconciliation;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.reconciliation.LogiflowReconciliationApi;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListEligibleCodQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListReconciliationsQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import com.tenantcore.logiflowservice.application.reconciliation.ReconciliationFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Reconciliation", description = "COD reconciliation management APIs")
public class LogiflowReconciliationControllerImpl implements LogiflowReconciliationApi {

    private final ReconciliationFacade reconciliationFacade;

    public LogiflowReconciliationControllerImpl(ReconciliationFacade reconciliationFacade) {
        this.reconciliationFacade = reconciliationFacade;
    }

    @Override
    @Operation(summary = "Create reconciliation", description = "Create a reconciliation batch from eligible COD record IDs")
    @PostMapping("/api/logiflow/reconciliations")
    public ApiResponse<ReconciliationResponse> createReconciliation(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @Valid @RequestBody CreateReconciliationRequest request
    ) {
        ReconciliationResponse response = reconciliationFacade.createReconciliation(resolveTenantCode(tenantCode), request);
        return ApiResponse.success("Reconciliation created", response);
    }

    @Override
    @Operation(summary = "List eligible COD records", description = "List COD records with COLLECTED status and not linked to reconciliation")
    @GetMapping("/api/logiflow/reconciliations/eligible-cod")
    public ApiResponse<PageResponse<EligibleCodRecordResponse>> listEligibleCodRecords(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        PageResponse<EligibleCodRecordResponse> response = reconciliationFacade.listEligibleCodRecords(
                resolveTenantCode(tenantCode),
                new ListEligibleCodQuery(page, size, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @Operation(summary = "List reconciliations", description = "List reconciliation batches by status/keyword with paging")
    @GetMapping("/api/logiflow/reconciliations")
    public ApiResponse<PageResponse<ReconciliationResponse>> listReconciliations(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        PageResponse<ReconciliationResponse> response = reconciliationFacade.listReconciliations(
                resolveTenantCode(tenantCode),
                new ListReconciliationsQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @Operation(summary = "Get reconciliation detail", description = "Get reconciliation by ID in current tenant")
    @GetMapping("/api/logiflow/reconciliations/{id}")
    public ApiResponse<ReconciliationResponse> getReconciliation(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        ReconciliationResponse response = reconciliationFacade.getReconciliation(resolveTenantCode(tenantCode), id);
        return ApiResponse.success(response);
    }

    @Override
    @Operation(summary = "Update reconciliation status", description = "Update reconciliation status to OPEN/RECONCILED/CANCELLED and sync COD records")
    @PatchMapping("/api/logiflow/reconciliations/{id}/status")
    public ApiResponse<ReconciliationResponse> updateReconciliationStatus(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReconciliationStatusRequest request
    ) {
        ReconciliationResponse response = reconciliationFacade.updateReconciliationStatus(resolveTenantCode(tenantCode), id, request);
        return ApiResponse.success("Reconciliation status updated", response);
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
