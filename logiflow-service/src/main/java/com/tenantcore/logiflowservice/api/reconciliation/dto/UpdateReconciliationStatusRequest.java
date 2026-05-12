package com.tenantcore.logiflowservice.api.reconciliation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateReconciliationStatusRequest(
        @NotBlank String status,
        String note
) {
}
