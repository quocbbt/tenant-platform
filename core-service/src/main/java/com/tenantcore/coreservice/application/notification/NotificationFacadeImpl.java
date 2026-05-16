package com.tenantcore.coreservice.application.notification;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.coreservice.api.notification.dto.ListNotificationsQuery;
import com.tenantcore.coreservice.api.notification.dto.NotificationResponse;
import com.tenantcore.coreservice.notification.NotificationService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationFacadeImpl implements NotificationFacade {

    private final NotificationService notificationService;

    public NotificationFacadeImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public PageResponse<NotificationResponse> listNotifications(String tenantCode, UUID userId, ListNotificationsQuery query) {
        return notificationService.listNotifications(tenantCode, userId, query);
    }

    @Override
    public NotificationResponse getNotification(String tenantCode, UUID userId, UUID notificationId) {
        return notificationService.getNotification(tenantCode, userId, notificationId);
    }

    @Override
    public long countUnread(String tenantCode, UUID userId) {
        return notificationService.countUnread(tenantCode, userId);
    }

    @Override
    public NotificationResponse markAsRead(String tenantCode, UUID userId, UUID notificationId) {
        return notificationService.markAsRead(tenantCode, userId, notificationId);
    }

    @Override
    public void markAllAsRead(String tenantCode, UUID userId) {
        notificationService.markAllAsRead(tenantCode, userId);
    }
}
