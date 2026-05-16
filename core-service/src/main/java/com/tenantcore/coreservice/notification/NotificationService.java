package com.tenantcore.coreservice.notification;

import com.tenantcore.common.context.UserContext;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.coreservice.api.notification.dto.ListNotificationsQuery;
import com.tenantcore.coreservice.api.notification.dto.NotificationResponse;
import com.tenantcore.coreservice.domain.NotificationEntity;
import com.tenantcore.coreservice.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class NotificationService {

    private static final String STATUS_UNREAD = "UNREAD";
    private static final String STATUS_READ = "READ";

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public PageResponse<NotificationResponse> listNotifications(String tenantCode, UUID userId, ListNotificationsQuery query) {
        requireNotificationViewPermission();

        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        String status = normalizeStatus(query.status());
        String keyword = blankToNull(query.keyword());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<NotificationResponse> pageResult = notificationRepository
                .search(tenantCode, userId, status, keyword, pageable)
                .map(this::toResponse);

        return PageResponse.of(
                pageResult.getContent(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements()
        );
    }

    public NotificationResponse getNotification(String tenantCode, UUID userId, UUID notificationId) {
        requireNotificationViewPermission();
        NotificationEntity entity = notificationRepository
                .findByIdAndTenantCodeAndReceiverUserId(notificationId, tenantCode, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Notification not found"));
        return toResponse(entity);
    }

    public long countUnread(String tenantCode, UUID userId) {
        requireNotificationViewPermission();
        return notificationRepository.countByTenantCodeAndReceiverUserIdAndStatus(tenantCode, userId, STATUS_UNREAD);
    }

    @Transactional
    public NotificationResponse markAsRead(String tenantCode, UUID userId, UUID notificationId) {
        requireNotificationViewPermission();
        NotificationEntity entity = notificationRepository
                .findByIdAndTenantCodeAndReceiverUserId(notificationId, tenantCode, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Notification not found"));

        if (!STATUS_READ.equals(entity.getStatus())) {
            entity.setStatus(STATUS_READ);
            entity.setReadAt(LocalDateTime.now());
            entity = notificationRepository.save(entity);
        }

        return toResponse(entity);
    }

    @Transactional
    public void markAllAsRead(String tenantCode, UUID userId) {
        requireNotificationViewPermission();
        notificationRepository.markAllAsRead(
                tenantCode,
                userId,
                STATUS_UNREAD,
                STATUS_READ,
                LocalDateTime.now()
        );
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getReceiverUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getType(),
                entity.getStatus(),
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }

    private void requireNotificationViewPermission() {
        if (!UserContext.hasPermission(SecurityConstants.PERMISSION_NOTIFICATION_VIEW)) {
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
    }

    private String normalizeStatus(String status) {
        String normalized = blankToNull(status);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
