package com.tenantcore.logiflowservice.api.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateReconciliationStatusRequest(
        @Schema(description = "OPEN/RECONCILED/CANCELLED", example = "RECONCILED")
        @NotBlank String status,
        @Schema(example = "Confirmed by accountant")
        String note
) {
}
