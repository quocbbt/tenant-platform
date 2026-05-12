package com.tenantcore.logiflowservice.reconciliation;

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
@Table(name = "logiflow_reconciliations")
public class ReconciliationEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "reconciliation_code", nullable = false, length = 100)
    private String reconciliationCode;

    @Column(name = "driver_id")
    private UUID driverId;

    @Column(name = "total_orders", nullable = false)
    private Integer totalOrders;

    @Column(name = "total_cod_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCodAmount;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "reconciled_at")
    private LocalDateTime reconciledAt;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
