package com.tenantcore.logiflowservice.reconciliation;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.EligibleCodRecordResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListEligibleCodQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ListReconciliationsQuery;
import com.tenantcore.logiflowservice.api.reconciliation.dto.ReconciliationResponse;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import com.tenantcore.logiflowservice.order.CodRecordEntity;
import com.tenantcore.logiflowservice.order.CodRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ReconciliationRepository reconciliationRepository;
    private final CodRecordRepository codRecordRepository;

    public ReconciliationService(
            ReconciliationRepository reconciliationRepository,
            CodRecordRepository codRecordRepository
    ) {
        this.reconciliationRepository = reconciliationRepository;
        this.codRecordRepository = codRecordRepository;
    }

    @Transactional
    public ReconciliationResponse createReconciliation(String tenantCode, CreateReconciliationRequest request) {
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
