package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    List<PermissionEntity> findByIdInAndStatus(Collection<UUID> ids, String status);
}
