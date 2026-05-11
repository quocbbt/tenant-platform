package com.tenantcore.logiflowservice.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "logiflow_delivery_assignments")
public class DeliveryAssignmentEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "assigned_at", insertable = false, updatable = false)
    private LocalDateTime assignedAt;

    @Column(columnDefinition = "TEXT")
    private String note;
}
