package com.tenantcore.logiflowservice.driver;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends JpaRepository<DriverEntity, UUID> {

    Optional<DriverEntity> findByIdAndTenantCodeAndDeletedAtIsNull(UUID id, String tenantCode);

    boolean existsByTenantCodeAndDriverCodeAndDeletedAtIsNull(String tenantCode, String driverCode);

    @Query("""
            select d
            from DriverEntity d
            where d.tenantCode = :tenantCode
              and d.deletedAt is null
              and (:status is null or d.status = :status)
              and (
                    :keyword is null
                    or lower(d.driverCode) like lower(concat('%', :keyword, '%'))
                    or lower(d.fullName) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(d.phone, '')) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<DriverEntity> search(
            @Param("tenantCode") String tenantCode,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
