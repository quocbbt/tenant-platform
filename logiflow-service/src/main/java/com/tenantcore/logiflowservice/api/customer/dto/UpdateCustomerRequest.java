package com.tenantcore.logiflowservice.api.customer.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCustomerRequest(
        @NotBlank String customerName,
        String phone,
        String email,
        String address,
        String type,
        String status
) {
}
