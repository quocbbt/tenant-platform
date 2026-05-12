package com.tenantcore.logiflowservice.api.customer.dto;

public record ListCustomersQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
