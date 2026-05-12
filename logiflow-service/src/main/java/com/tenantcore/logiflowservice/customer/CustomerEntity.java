package com.tenantcore.logiflowservice.customer;

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
@Table(name = "logiflow_customers")
public class CustomerEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_code", nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "customer_code", nullable = false, length = 100)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 255)
    private String customerName;

    @Column(length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(length = 30)
    private String type;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
