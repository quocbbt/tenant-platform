package com.tenantcore.logiflowservice.api.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String customerCode,
        @NotBlank String customerName,
        String phone,
        String email,
        String address,
        String type
) {
}
