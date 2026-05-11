package com.tenantcore.logiflowservice.api.order.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateCodRequest(
        @NotNull BigDecimal amount,
        String status,
        String note
) {
}
