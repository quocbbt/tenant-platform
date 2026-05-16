package com.tenantcore.coreservice.application.notification;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.coreservice.api.notification.dto.ListNotificationsQuery;
import com.tenantcore.coreservice.api.notification.dto.NotificationResponse;

import java.util.UUID;

public interface NotificationFacade {

    PageResponse<NotificationResponse> listNotifications(String tenantCode, UUID userId, ListNotificationsQuery query);

    NotificationResponse getNotification(String tenantCode, UUID userId, UUID notificationId);

    long countUnread(String tenantCode, UUID userId);

    NotificationResponse markAsRead(String tenantCode, UUID userId, UUID notificationId);

    void markAllAsRead(String tenantCode, UUID userId);
}
