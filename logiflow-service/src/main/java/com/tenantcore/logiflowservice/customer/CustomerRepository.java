package com.tenantcore.logiflowservice.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<CustomerEntity, UUID> {

    Optional<CustomerEntity> findByIdAndTenantCodeAndDeletedAtIsNull(UUID id, String tenantCode);

    boolean existsByTenantCodeAndCustomerCodeAndDeletedAtIsNull(String tenantCode, String customerCode);

    @Query("""
            select c
            from CustomerEntity c
            where c.tenantCode = :tenantCode
              and c.deletedAt is null
              and (:status is null or c.status = :status)
              and (
                    :keyword is null
                    or lower(c.customerCode) like lower(concat('%', :keyword, '%'))
                    or lower(c.customerName) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(c.phone, '')) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<CustomerEntity> search(
            @Param("tenantCode") String tenantCode,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
