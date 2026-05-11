package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByTenantCodeAndUsernameAndStatusAndDeletedAtIsNull(String tenantCode, String username, String status);

    Optional<UserEntity> findByIdAndTenantCodeAndStatusAndDeletedAtIsNull(UUID id, String tenantCode, String status);
}
