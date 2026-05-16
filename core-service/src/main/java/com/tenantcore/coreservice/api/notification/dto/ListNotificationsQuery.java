package com.tenantcore.coreservice.api.notification.dto;

public record ListNotificationsQuery(
        int page,
        int size,
        String status,
        String keyword
) {
}
