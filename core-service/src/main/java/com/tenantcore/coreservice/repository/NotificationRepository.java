package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    @Query("""
            select n
            from NotificationEntity n
            where n.tenantCode = :tenantCode
              and n.receiverUserId = :receiverUserId
              and (:status is null or n.status = :status)
              and (
                    :keyword is null
                    or lower(n.title) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(n.content, '')) like lower(concat('%', :keyword, '%'))
                    or lower(n.type) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<NotificationEntity> search(
            @Param("tenantCode") String tenantCode,
            @Param("receiverUserId") UUID receiverUserId,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    Optional<NotificationEntity> findByIdAndTenantCodeAndReceiverUserId(UUID id, String tenantCode, UUID receiverUserId);

    long countByTenantCodeAndReceiverUserIdAndStatus(String tenantCode, UUID receiverUserId, String status);

    @Modifying
    @Query("""
            update NotificationEntity n
            set n.status = :readStatus,
                n.readAt = :readAt
            where n.tenantCode = :tenantCode
              and n.receiverUserId = :receiverUserId
              and n.status = :unreadStatus
            """)
    int markAllAsRead(
            @Param("tenantCode") String tenantCode,
            @Param("receiverUserId") UUID receiverUserId,
            @Param("unreadStatus") String unreadStatus,
            @Param("readStatus") String readStatus,
            @Param("readAt") LocalDateTime readAt
    );
}
