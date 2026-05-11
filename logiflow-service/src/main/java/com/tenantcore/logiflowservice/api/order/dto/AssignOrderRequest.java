package com.tenantcore.logiflowservice.api.order.dto;

import java.util.UUID;

public record AssignOrderRequest(
        UUID driverId,
        UUID vehicleId,
        String note
) {
}
