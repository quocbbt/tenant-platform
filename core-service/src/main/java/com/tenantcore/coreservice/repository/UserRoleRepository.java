package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {

    @Query("select ur.roleId from UserRoleEntity ur where ur.tenantCode = :tenantCode and ur.userId = :userId")
    List<UUID> findRoleIdsByTenantCodeAndUserId(@Param("tenantCode") String tenantCode, @Param("userId") UUID userId);
}
