package com.tenantcore.logiflowservice.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CodRecordRepository extends JpaRepository<CodRecordEntity, UUID> {

    @Query("""
            select
              count(c) as totalRecords,
              coalesce(sum(c.amount), 0) as totalAmount,
              coalesce(sum(case when c.status = 'PENDING' then c.amount else 0 end), 0) as pendingAmount,
              coalesce(sum(case when c.status = 'COLLECTED' then c.amount else 0 end), 0) as collectedAmount,
              coalesce(sum(case when c.status = 'RECONCILED' then c.amount else 0 end), 0) as reconciledAmount
            from CodRecordEntity c
            where c.tenantCode = :tenantCode
            """)
    CodSummaryProjection summarizeByTenant(@Param("tenantCode") String tenantCode);

    @Query("""
            select
              cast(c.createdAt as date) as businessDate,
              count(c) as totalRecords,
              coalesce(sum(c.amount), 0) as totalAmount,
              coalesce(sum(case when c.status = 'PENDING' then c.amount else 0 end), 0) as pendingAmount,
              coalesce(sum(case when c.status = 'COLLECTED' then c.amount else 0 end), 0) as collectedAmount,
              coalesce(sum(case when c.status = 'RECONCILED' then c.amount else 0 end), 0) as reconciledAmount
            from CodRecordEntity c
            where c.tenantCode = :tenantCode
              and c.createdAt >= :fromDateTime
              and c.createdAt < :toDateTimeExclusive
            group by cast(c.createdAt as date)
            order by cast(c.createdAt as date) desc
            """)
    List<CodDailyProjection> summarizeDailyByTenant(
            @Param("tenantCode") String tenantCode,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTimeExclusive") LocalDateTime toDateTimeExclusive
    );

    List<CodRecordEntity> findByTenantCodeAndIdIn(String tenantCode, List<UUID> ids);

    List<CodRecordEntity> findByTenantCodeAndReconciliationId(String tenantCode, UUID reconciliationId);

    Page<CodRecordEntity> findByTenantCodeAndStatusAndReconciliationIdIsNull(
            String tenantCode,
            String status,
            Pageable pageable
    );

    @Query("""
            select c
            from CodRecordEntity c
            where c.tenantCode = :tenantCode
              and c.status = 'COLLECTED'
              and c.reconciliationId is null
              and (
                    cast(c.orderId as string) like concat('%', :keyword, '%')
                    or cast(c.id as string) like concat('%', :keyword, '%')
                  )
            """)
    Page<CodRecordEntity> listEligibleForReconciliation(
            @Param("tenantCode") String tenantCode,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
