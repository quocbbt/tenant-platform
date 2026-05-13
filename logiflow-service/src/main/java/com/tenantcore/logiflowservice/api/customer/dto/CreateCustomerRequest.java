package com.tenantcore.logiflowservice.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @Schema(description = "Unique customer code in tenant", example = "CUS-001")
        @NotBlank String customerCode,
        @Schema(description = "Customer display name", example = "Nguyen Van A")
        @NotBlank String customerName,
        @Schema(example = "0909123456")
        String phone,
        @Schema(example = "customer@example.com")
        String email,
        @Schema(example = "District 1, Ho Chi Minh City")
        String address,
        @Schema(description = "Customer type", example = "NORMAL")
        String type
) {
}
