package com.tenantcore.logiflowservice.reconciliation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<ReconciliationEntity, UUID> {

    Optional<ReconciliationEntity> findByIdAndTenantCodeAndDeletedAtIsNull(UUID id, String tenantCode);

    @Query("""
            select r
            from ReconciliationEntity r
            where r.tenantCode = :tenantCode
              and r.deletedAt is null
              and (:status is null or r.status = :status)
              and (
                    :keyword is null
                    or lower(r.reconciliationCode) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(r.note, '')) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<ReconciliationEntity> search(
            @Param("tenantCode") String tenantCode,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select
              r.driverId as driverId,
              count(r) as totalReconciliations,
              coalesce(sum(r.totalOrders), 0) as totalOrders,
              coalesce(sum(r.totalCodAmount), 0) as totalCodAmount,
              coalesce(sum(case when r.status = 'RECONCILED' then 1 else 0 end), 0) as reconciledCount
            from ReconciliationEntity r
            where r.tenantCode = :tenantCode
              and r.deletedAt is null
              and r.createdAt >= :fromDateTime
              and r.createdAt < :toDateTimeExclusive
            group by r.driverId
            order by coalesce(sum(r.totalCodAmount), 0) desc
            """)
    List<ReconciliationDriverProjection> summarizeByDriver(
            @Param("tenantCode") String tenantCode,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive
    );
}
