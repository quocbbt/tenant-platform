package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    List<RoleEntity> findByIdInAndTenantCodeAndStatus(Collection<UUID> ids, String tenantCode, String status);
}
