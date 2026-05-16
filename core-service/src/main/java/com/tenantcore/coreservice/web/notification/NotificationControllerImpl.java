package com.tenantcore.coreservice.web.notification;

import com.tenantcore.common.context.UserContext;
import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.coreservice.api.notification.NotificationApi;
import com.tenantcore.coreservice.api.notification.dto.ListNotificationsQuery;
import com.tenantcore.coreservice.api.notification.dto.NotificationResponse;
import com.tenantcore.coreservice.api.notification.dto.UnreadCountResponse;
import com.tenantcore.coreservice.application.notification.NotificationFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
public class NotificationControllerImpl implements NotificationApi {

    private final NotificationFacade notificationFacade;

    public NotificationControllerImpl(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @Override
    @GetMapping("/api/notifications")
    public ApiResponse<PageResponse<NotificationResponse>> listNotifications(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        UserContext.CurrentUser currentUser = requireCurrentUser();
        String effectiveTenant = resolveTenantCode(currentUser, tenantCode);
        PageResponse<NotificationResponse> response = notificationFacade.listNotifications(
                effectiveTenant,
                currentUser.userId(),
                new ListNotificationsQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/notifications/{id}")
    public ApiResponse<NotificationResponse> getNotification(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        UserContext.CurrentUser currentUser = requireCurrentUser();
        String effectiveTenant = resolveTenantCode(currentUser, tenantCode);
        NotificationResponse response = notificationFacade.getNotification(effectiveTenant, currentUser.userId(), id);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/notifications/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        UserContext.CurrentUser currentUser = requireCurrentUser();
        String effectiveTenant = resolveTenantCode(currentUser, tenantCode);
        long unreadCount = notificationFacade.countUnread(effectiveTenant, currentUser.userId());
        return ApiResponse.success(new UnreadCountResponse(unreadCount));
    }

    @Override
    @PutMapping("/api/notifications/{id}/read")
    public ApiResponse<NotificationResponse> markAsRead(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        UserContext.CurrentUser currentUser = requireCurrentUser();
        String effectiveTenant = resolveTenantCode(currentUser, tenantCode);
        NotificationResponse response = notificationFacade.markAsRead(effectiveTenant, currentUser.userId(), id);
        return ApiResponse.success("Notification marked as read", response);
    }

    @Override
    @PutMapping("/api/notifications/read-all")
    public ApiResponse<String> markAllAsRead(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        UserContext.CurrentUser currentUser = requireCurrentUser();
        String effectiveTenant = resolveTenantCode(currentUser, tenantCode);
        notificationFacade.markAllAsRead(effectiveTenant, currentUser.userId());
        return ApiResponse.success("All notifications marked as read", "OK");
    }

    private UserContext.CurrentUser requireCurrentUser() {
        UserContext.CurrentUser currentUser = UserContext.getCurrentUser();
        if (currentUser == null || currentUser.userId() == null || currentUser.tenantCode() == null || currentUser.tenantCode().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return currentUser;
    }

    private String resolveTenantCode(UserContext.CurrentUser currentUser, String tenantCodeHeader) {
        if (tenantCodeHeader == null || tenantCodeHeader.isBlank()) {
            return currentUser.tenantCode();
        }
        if (!tenantCodeHeader.equals(currentUser.tenantCode())) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }
        return tenantCodeHeader;
    }
}
