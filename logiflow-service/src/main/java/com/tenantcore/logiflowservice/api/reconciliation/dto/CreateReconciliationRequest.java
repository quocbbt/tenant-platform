package com.tenantcore.logiflowservice.api.reconciliation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateReconciliationRequest(
        UUID driverId,
        @NotEmpty List<UUID> codRecordIds,
        String note
) {
}
