package com.tenantcore.logiflowservice.api.reconciliation;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

@Tag(name = "Reconciliation", description = "COD reconciliation APIs")
public interface LogiflowReconciliationApi {

    @Operation(summary = "Create reconciliation batch")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reconciliation created",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"status\":\"SUCCESS\",\"message\":\"Reconciliation created\",\"data\":{\"id\":\"8fd22eea-e38d-42f8-ae9c-f8e97d9d6f88\",\"tenantCode\":\"demo-tenant\",\"reconciliationCode\":\"REC-1747098654000\",\"driverId\":\"11111111-1111-1111-1111-111111111111\",\"totalOrders\":1,\"totalCodAmount\":120000,\"status\":\"OPEN\",\"reconciledAt\":null,\"note\":\"Daily reconciliation batch\",\"createdAt\":\"2026-05-13T13:30:00\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"status\":\"ERROR\",\"message\":\"COD record outside reconciliation time window: 00000000-0000-0000-0000-000000000001\",\"data\":null}")
                    )
            )
    })
    ApiResponse<ReconciliationResponse> createReconciliation(
            String tenantCode,
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"driverId\":\"11111111-1111-1111-1111-111111111111\",\"codRecordIds\":[\"00000000-0000-0000-0000-000000000001\"],\"note\":\"Daily reconciliation batch\"}")))
            @Valid CreateReconciliationRequest request
    );

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

    @Operation(summary = "Update reconciliation status")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Reconciliation status updated",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"status\":\"SUCCESS\",\"message\":\"Reconciliation status updated\",\"data\":{\"id\":\"8fd22eea-e38d-42f8-ae9c-f8e97d9d6f88\",\"tenantCode\":\"demo-tenant\",\"reconciliationCode\":\"REC-1747098654000\",\"driverId\":\"11111111-1111-1111-1111-111111111111\",\"totalOrders\":1,\"totalCodAmount\":120000,\"status\":\"RECONCILED\",\"reconciledAt\":\"2026-05-13T14:00:00\",\"note\":\"Confirmed by accountant\",\"createdAt\":\"2026-05-13T13:30:00\"}}")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Reconciliation not found",
                    content = @Content(
                            examples = @ExampleObject(value = "{\"status\":\"ERROR\",\"message\":\"Reconciliation not found\",\"data\":null}")
                    )
            )
    })
    ApiResponse<ReconciliationResponse> updateReconciliationStatus(
            String tenantCode,
            UUID id,
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"status\":\"RECONCILED\",\"note\":\"Confirmed by accountant\"}")))
            @Valid UpdateReconciliationStatusRequest request
    );
}
