package com.tenantcore.logiflowservice.api.order.dto;

public record ListOrdersQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
