package com.tenantcore.logiflowservice.api.order.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateOrderStatusRequest(
        @NotBlank String status,
        String reason
) {
}
