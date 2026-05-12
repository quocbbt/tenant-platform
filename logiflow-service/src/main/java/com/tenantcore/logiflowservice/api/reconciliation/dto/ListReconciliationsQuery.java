package com.tenantcore.logiflowservice.api.reconciliation.dto;

public record ListReconciliationsQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
