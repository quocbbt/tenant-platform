package com.tenantcore.logiflowservice.api.reconciliation.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreateReconciliationRequest(
        @Schema(description = "Optional driver reference", example = "11111111-1111-1111-1111-111111111111")
        UUID driverId,
        @ArraySchema(schema = @Schema(example = "00000000-0000-0000-0000-000000000001"))
        @NotEmpty List<UUID> codRecordIds,
        @Schema(example = "Daily reconciliation batch")
        String note
) {
}
