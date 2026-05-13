package com.tenantcore.logiflowservice.reconciliation;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListEligibleCodQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListReconciliationsQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import com.tenantcore.logiflowservice.driver.DriverEntity;
import com.tenantcore.logiflowservice.driver.DriverRepository;
import com.tenantcore.logiflowservice.order.CodRecordEntity;
import com.tenantcore.logiflowservice.order.CodRecordRepository;
import com.tenantcore.logiflowservice.order.DeliveryAssignmentEntity;
import com.tenantcore.logiflowservice.order.DeliveryAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final CodRecordRepository codRecordRepository;
    private final DriverRepository driverRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final ReconciliationPolicyProperties reconciliationPolicyProperties;

    public ReconciliationService(
            ReconciliationRepository reconciliationRepository,
            CodRecordRepository codRecordRepository,
            DriverRepository driverRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            ReconciliationPolicyProperties reconciliationPolicyProperties
    ) {
        this.reconciliationRepository = reconciliationRepository;
        this.codRecordRepository = codRecordRepository;
        this.driverRepository = driverRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.reconciliationPolicyProperties = reconciliationPolicyProperties;
    }

    @Transactional
    public ReconciliationResponse createReconciliation(String tenantCode, CreateReconciliationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        List<UUID> uniqueIds = request.codRecordIds().stream().distinct().toList();
        List<CodRecordEntity> codRecords = codRecordRepository.findByTenantCodeAndIdIn(tenantCode, uniqueIds);
        if (codRecords.size() != uniqueIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Some COD records are invalid for tenant");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        HashSet<UUID> orderIds = new HashSet<>();
        for (CodRecordEntity cod : codRecords) {
            if (!"COLLECTED".equals(cod.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "COD record must be in COLLECTED status");
            }
            if (cod.getReconciliationId() != null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "COD record already linked to reconciliation");
            }
            totalAmount = totalAmount.add(cod.getAmount());
            orderIds.add(cod.getOrderId());
        }

        validateCodTimeWindow(codRecords, now);
        if (request.driverId() != null) {
            validateDriverPolicy(tenantCode, request.driverId(), codRecords);
        }

        ReconciliationEntity entity = new ReconciliationEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setReconciliationCode("REC-" + System.currentTimeMillis());
        entity.setDriverId(request.driverId());
        entity.setTotalOrders(orderIds.size());
        entity.setTotalCodAmount(totalAmount);
        entity.setStatus("OPEN");
        entity.setNote(blankToNull(request.note()));
        ReconciliationEntity saved = reconciliationRepository.save(entity);

        for (CodRecordEntity cod : codRecords) {
            cod.setReconciliationId(saved.getId());
        }
        codRecordRepository.saveAll(codRecords);
        return toResponse(saved);
    }

    public Page<EligibleCodRecordResponse> listEligibleCodRecords(String tenantCode, ListEligibleCodQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String keyword = blankToNull(query.keyword());
        Page<CodRecordEntity> records = keyword == null
                ? codRecordRepository.findByTenantCodeAndStatusAndReconciliationIdIsNull(tenantCode, "COLLECTED", pageable)
                : codRecordRepository.listEligibleForReconciliation(tenantCode, keyword, pageable);
        return records
                .map(c -> new EligibleCodRecordResponse(
                        c.getId(),
                        c.getOrderId(),
                        c.getAmount(),
                        c.getStatus(),
                        c.getCreatedAt()
                ));
    }

    public Page<ReconciliationResponse> listReconciliations(String tenantCode, ListReconciliationsQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reconciliationRepository.search(tenantCode, blankToNull(query.status()), blankToNull(query.keyword()), pageable)
                .map(this::toResponse);
    }

    public ReconciliationResponse getReconciliation(String tenantCode, UUID id) {
        ReconciliationEntity entity = reconciliationRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Reconciliation not found"));
        return toResponse(entity);
    }

    @Transactional
    public ReconciliationResponse updateReconciliationStatus(String tenantCode, UUID id, UpdateReconciliationStatusRequest request) {
        ReconciliationEntity entity = reconciliationRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Reconciliation not found"));

        String status = normalizeStatus(request.status());
        entity.setStatus(status);
        entity.setNote(request.note() == null || request.note().isBlank() ? entity.getNote() : request.note().trim());
        if ("RECONCILED".equals(status)) {
            LocalDateTime now = LocalDateTime.now();
            entity.setReconciledAt(now);
            List<CodRecordEntity> records = codRecordRepository.findByTenantCodeAndReconciliationId(tenantCode, id);
            for (CodRecordEntity cod : records) {
                cod.setStatus("RECONCILED");
                cod.setReconciledAt(now);
            }
            codRecordRepository.saveAll(records);
        } else if ("CANCELLED".equals(status)) {
            entity.setReconciledAt(null);
            List<CodRecordEntity> records = codRecordRepository.findByTenantCodeAndReconciliationId(tenantCode, id);
            for (CodRecordEntity cod : records) {
                cod.setReconciliationId(null);
                cod.setStatus("COLLECTED");
                cod.setReconciledAt(null);
            }
            codRecordRepository.saveAll(records);
        }
        return toResponse(reconciliationRepository.save(entity));
    }

    private void validateCodTimeWindow(List<CodRecordEntity> codRecords, LocalDateTime now) {
        int maxCodAgeHours = reconciliationPolicyProperties.normalizedMaxCodAgeHours();
        if (maxCodAgeHours == 0) {
            return;
        }
        LocalDateTime windowStart = now.minusHours(maxCodAgeHours);
        for (CodRecordEntity cod : codRecords) {
            LocalDateTime codTimestamp = resolveCodTimestamp(cod);
            if (codTimestamp.isBefore(windowStart)) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "COD record outside reconciliation time window: " + cod.getId()
                );
            }
        }
    }

    private void validateDriverPolicy(String tenantCode, UUID driverId, List<CodRecordEntity> codRecords) {
        DriverEntity driver = driverRepository.findByIdAndTenantCodeAndDeletedAtIsNull(driverId, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "Driver is invalid for tenant"));
        if (!"ACTIVE".equals(driver.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Driver must be ACTIVE for reconciliation");
        }

        if (!reconciliationPolicyProperties.enforceDriverAssignment()) {
            return;
        }

        List<UUID> orderIds = codRecords.stream().map(CodRecordEntity::getOrderId).distinct().toList();
        List<DeliveryAssignmentEntity> assignments = deliveryAssignmentRepository
                .findByTenantCodeAndOrderIdInAndDeletedAtIsNullOrderByAssignedAtDesc(tenantCode, orderIds);

        Map<UUID, DeliveryAssignmentEntity> latestAssignmentByOrder = new HashMap<>();
        for (DeliveryAssignmentEntity assignment : assignments) {
            latestAssignmentByOrder.putIfAbsent(assignment.getOrderId(), assignment);
        }

        for (CodRecordEntity cod : codRecords) {
            DeliveryAssignmentEntity assignment = latestAssignmentByOrder.get(cod.getOrderId());
            if (assignment == null || assignment.getDriverId() == null || !driverId.equals(assignment.getDriverId())) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "COD record does not belong to selected driver assignment: " + cod.getId()
                );
            }
        }
    }

    private ReconciliationResponse toResponse(ReconciliationEntity entity) {
        return new ReconciliationResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getReconciliationCode(),
                entity.getDriverId(),
                entity.getTotalOrders(),
                entity.getTotalCodAmount(),
                entity.getStatus(),
                entity.getReconciledAt(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime resolveCodTimestamp(CodRecordEntity cod) {
        if (cod.getCreatedAt() != null) {
            return cod.getCreatedAt();
        }
        if (cod.getUpdatedAt() != null) {
            return cod.getUpdatedAt();
        }
        return LocalDateTime.now();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Status is required");
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "OPEN", "RECONCILED", "CANCELLED" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported reconciliation status: " + status);
        };
    }
}
