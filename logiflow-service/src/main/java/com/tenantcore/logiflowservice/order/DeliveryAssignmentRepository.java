package com.tenantcore.logiflowservice.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignmentEntity, UUID> {
}
