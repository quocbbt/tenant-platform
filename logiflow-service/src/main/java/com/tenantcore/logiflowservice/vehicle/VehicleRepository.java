package com.tenantcore.logiflowservice.vehicle;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<VehicleEntity, UUID> {

    Optional<VehicleEntity> findByIdAndTenantCodeAndDeletedAtIsNull(UUID id, String tenantCode);

    boolean existsByTenantCodeAndVehicleCodeAndDeletedAtIsNull(String tenantCode, String vehicleCode);

    boolean existsByTenantCodeAndPlateNumberAndDeletedAtIsNull(String tenantCode, String plateNumber);

    @Query("""
            select v
            from VehicleEntity v
            where v.tenantCode = :tenantCode
              and v.deletedAt is null
              and (:status is null or v.status = :status)
              and (
                    :keyword is null
                    or lower(v.vehicleCode) like lower(concat('%', :keyword, '%'))
                    or lower(v.plateNumber) like lower(concat('%', :keyword, '%'))
                    or lower(coalesce(v.vehicleType, '')) like lower(concat('%', :keyword, '%'))
                  )
            """)
    Page<VehicleEntity> search(
            @Param("tenantCode") String tenantCode,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
