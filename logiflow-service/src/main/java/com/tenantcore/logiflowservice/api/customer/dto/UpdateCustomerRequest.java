package com.tenantcore.logiflowservice.api.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRequest(
        @Schema(description = "Updated customer name", example = "Nguyen Van B")
        @NotBlank String customerName,
        @Schema(example = "0909888777")
        String phone,
        @Schema(example = "customer.updated@example.com")
        String email,
        @Schema(example = "District 7, Ho Chi Minh City")
        String address,
        @Schema(example = "VIP")
        String type,
        @Schema(description = "ACTIVE/INACTIVE", example = "ACTIVE")
        String status
) {
}
