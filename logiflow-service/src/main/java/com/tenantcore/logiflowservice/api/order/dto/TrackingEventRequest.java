package com.tenantcore.logiflowservice.api.order.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrackingEventRequest(
        @NotBlank String eventCode,
        @NotBlank String eventName,
        String description,
        String locationText,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime eventTime
) {
}
