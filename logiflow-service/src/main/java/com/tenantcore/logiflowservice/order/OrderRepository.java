package com.tenantcore.logiflowservice.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID> {

    Optional<OrderEntity> findByIdAndTenantCode(UUID id, String tenantCode);

    Page<OrderEntity> findByTenantCode(String tenantCode, Pageable pageable);

    Page<OrderEntity> findByTenantCodeAndStatus(String tenantCode, String status, Pageable pageable);

    @Query("""
            select o
            from OrderEntity o
            where o.tenantCode = :tenantCode
              and (
                    lower(o.orderCode) like lower(concat('%', :keyword, '%'))
                    or lower(o.receiverName) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<OrderEntity> searchByTenantAndKeyword(
            @Param("tenantCode") String tenantCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select o
            from OrderEntity o
            where o.tenantCode = :tenantCode
              and o.status = :status
              and (
                    lower(o.orderCode) like lower(concat('%', :keyword, '%'))
                    or lower(o.receiverName) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<OrderEntity> searchByTenantAndStatusAndKeyword(
            @Param("tenantCode") String tenantCode,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
