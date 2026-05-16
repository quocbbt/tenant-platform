package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByTenantCodeAndUsernameAndStatusAndDeletedAtIsNull(String tenantCode, String username, String status);

    Optional<UserEntity> findByIdAndTenantCodeAndStatusAndDeletedAtIsNull(UUID id, String tenantCode, String status);

    @Query("""
            select u
            from UserEntity u
            where u.tenantCode = :tenantCode
              and u.status = :status
              and u.deletedAt is null
              and (
                    lower(u.username) = lower(:identifier)
                    or lower(u.email) = lower(:identifier)
                    or u.phone = :identifier
                  )
            """)
    Optional<UserEntity> findByTenantCodeAndIdentifierAndStatusAndDeletedAtIsNull(
            @Param("tenantCode") String tenantCode,
            @Param("identifier") String identifier,
            @Param("status") String status
    );
}
