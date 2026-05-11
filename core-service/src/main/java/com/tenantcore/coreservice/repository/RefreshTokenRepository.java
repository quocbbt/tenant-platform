package com.tenantcore.coreservice.repository;

import com.tenantcore.coreservice.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHashAndStatus(String tokenHash, String status);

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);
}
