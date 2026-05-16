package com.tenantcore.coreservice.api.notification;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.coreservice.api.notification.dto.NotificationResponse;
import com.tenantcore.coreservice.api.notification.dto.UnreadCountResponse;

import java.util.UUID;

public interface NotificationApi {

    ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<NotificationResponse> getNotification(String tenantCode, UUID id);

    ApiResponse<UnreadCountResponse> getUnreadCount(String tenantCode);

    ApiResponse<NotificationResponse> markAsRead(String tenantCode, UUID id);

    ApiResponse<String> markAllAsRead(String tenantCode);
}
