package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query("select rp.permissionId from RolePermissionEntity rp where rp.roleId in :roleIds")
    List<UUID> findPermissionIdsByRoleIds(@Param("roleIds") Collection<UUID> roleIds);
}
