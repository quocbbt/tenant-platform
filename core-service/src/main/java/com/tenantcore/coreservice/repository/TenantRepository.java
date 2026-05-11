package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<TenantEntity, UUID> {

    Optional<TenantEntity> findByTenantCodeAndStatus(String tenantCode, String status);
}
