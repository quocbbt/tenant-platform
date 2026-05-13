package com.tenantcore.logiflowservice.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @Schema(description = "NEW/ASSIGNED/IN_TRANSIT/COMPLETED/CANCELLED", example = "COMPLETED")
        @NotBlank String status,
        @Schema(example = "Delivered successfully")
        String reason
) {
}
