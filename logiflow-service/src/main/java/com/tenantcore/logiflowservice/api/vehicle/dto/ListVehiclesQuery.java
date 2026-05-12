package com.tenantcore.logiflowservice.api.vehicle.dto;

public record ListVehiclesQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
