package com.tenantcore.logiflowservice.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "logiflow_vehicles")
public class VehicleEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "vehicle_code", nullable = false, length = 100)
    private String vehicleCode;

    @Column(name = "plate_number", nullable = false, length = 50)
    private String plateNumber;

    @Column(name = "vehicle_type", length = 50)
    private String vehicleType;

    @Column(name = "capacity_kg", precision = 12, scale = 2)
    private BigDecimal capacityKg;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
