package com.tenantcore.logiflowservice.api.driver.dto;

public record ListDriversQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
