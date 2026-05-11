package com.tenantcore.logiflowservice.api.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank String receiverName,
        @NotBlank String receiverAddress,
        @NotNull BigDecimal codAmount
) {
}
