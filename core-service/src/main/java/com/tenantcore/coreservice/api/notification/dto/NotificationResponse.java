package com.tenantcore.coreservice.api.notification.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String tenantCode,
        UUID receiverUserId,
        String title,
        String content,
        String type,
        String status,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
