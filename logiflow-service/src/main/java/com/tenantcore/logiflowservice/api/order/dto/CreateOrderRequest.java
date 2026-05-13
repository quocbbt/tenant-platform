package com.tenantcore.logiflowservice.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @Schema(example = "Le Thi B")
        @NotBlank String receiverName,
        @Schema(example = "Da Nang City")
        @NotBlank String receiverAddress,
        @Schema(example = "120000")
        @NotNull BigDecimal codAmount
) {
}
