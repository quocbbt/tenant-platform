package com.tenantcore.logiflowservice.api.reconciliation.dto;

public record ListEligibleCodQuery(
        int page,
        int size,
        String keyword
) {
}
