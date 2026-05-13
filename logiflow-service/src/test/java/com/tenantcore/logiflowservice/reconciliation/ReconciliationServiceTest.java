package com.tenantcore.logiflowservice.reconciliation;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.logiflowservice.api.reconciliation.dto.CreateReconciliationRequest;
import com.tenantcore.logiflowservice.api.reconciliation.dto.UpdateReconciliationStatusRequest;
import com.tenantcore.logiflowservice.driver.DriverEntity;
import com.tenantcore.logiflowservice.driver.DriverRepository;
import com.tenantcore.logiflowservice.order.CodRecordEntity;
import com.tenantcore.logiflowservice.order.CodRecordRepository;
import com.tenantcore.logiflowservice.order.DeliveryAssignmentEntity;
import com.tenantcore.logiflowservice.order.DeliveryAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private ReconciliationRepository reconciliationRepository;

    @Mock
    private CodRecordRepository codRecordRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private DeliveryAssignmentRepository deliveryAssignmentRepository;

    @Mock
    private ReconciliationPolicyProperties reconciliationPolicyProperties;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @BeforeEach
    void setUp() {
        when(reconciliationPolicyProperties.normalizedMaxCodAgeHours()).thenReturn(72);
        when(reconciliationPolicyProperties.enforceDriverAssignment()).thenReturn(true);
    }

    @Test
    void createReconciliation_shouldLinkCodRecordsAndAggregateAmount() {
        String tenantCode = "demo";
        UUID cod1 = UUID.randomUUID();
        UUID cod2 = UUID.randomUUID();
        UUID order1 = UUID.randomUUID();
        UUID order2 = UUID.randomUUID();

        CodRecordEntity rec1 = cod(cod1, order1, "COLLECTED", new BigDecimal("120000"));
        CodRecordEntity rec2 = cod(cod2, order2, "COLLECTED", new BigDecimal("30000"));

        when(codRecordRepository.findByTenantCodeAndIdIn(eq(tenantCode), any())).thenReturn(List.of(rec1, rec2));
        when(reconciliationRepository.save(any(ReconciliationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = reconciliationService.createReconciliation(
                tenantCode,
                new CreateReconciliationRequest(null, List.of(cod1, cod2), "batch")
        );

        assertEquals("OPEN", result.status());
        assertEquals(2, result.totalOrders());
        assertEquals(new BigDecimal("150000"), result.totalCodAmount());
        assertNotNull(result.id());

        ArgumentCaptor<List<CodRecordEntity>> codCaptor = ArgumentCaptor.forClass(List.class);
        verify(codRecordRepository).saveAll(codCaptor.capture());
        for (CodRecordEntity cod : codCaptor.getValue()) {
            assertEquals(result.id(), cod.getReconciliationId());
        }
    }

    @Test
    void createReconciliation_shouldRejectNonCollectedCodRecord() {
        String tenantCode = "demo";
        UUID codId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        CodRecordEntity rec = cod(codId, orderId, "PENDING", new BigDecimal("10000"));
        when(codRecordRepository.findByTenantCodeAndIdIn(eq(tenantCode), any())).thenReturn(List.of(rec));

        assertThrows(
                BusinessException.class,
                () -> reconciliationService.createReconciliation(
                        tenantCode,
                        new CreateReconciliationRequest(null, List.of(codId), null)
                )
        );
    }

    @Test
    void createReconciliation_shouldRejectExpiredCodRecord() {
        String tenantCode = "demo";
        UUID codId = UUID.randomUUID();
        CodRecordEntity rec = cod(codId, UUID.randomUUID(), "COLLECTED", new BigDecimal("10000"));
        rec.setCreatedAt(LocalDateTime.now().minusHours(100));
        when(codRecordRepository.findByTenantCodeAndIdIn(eq(tenantCode), any())).thenReturn(List.of(rec));

        assertThrows(
                BusinessException.class,
                () -> reconciliationService.createReconciliation(
                        tenantCode,
                        new CreateReconciliationRequest(null, List.of(codId), null)
                )
        );
    }

    @Test
    void createReconciliation_shouldRejectDriverMismatchAssignment() {
        String tenantCode = "demo";
        UUID codId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID requestedDriverId = UUID.randomUUID();
        UUID assignedDriverId = UUID.randomUUID();

        CodRecordEntity rec = cod(codId, orderId, "COLLECTED", new BigDecimal("10000"));
        DriverEntity driver = new DriverEntity();
        driver.setId(requestedDriverId);
        driver.setTenantCode(tenantCode);
        driver.setStatus("ACTIVE");

        when(codRecordRepository.findByTenantCodeAndIdIn(eq(tenantCode), any())).thenReturn(List.of(rec));
        when(driverRepository.findByIdAndTenantCodeAndDeletedAtIsNull(requestedDriverId, tenantCode))
                .thenReturn(java.util.Optional.of(driver));
        when(deliveryAssignmentRepository.findByTenantCodeAndOrderIdInAndDeletedAtIsNullOrderByAssignedAtDesc(eq(tenantCode), any()))
                .thenReturn(List.of(assignment(orderId, assignedDriverId)));

        assertThrows(
                BusinessException.class,
                () -> reconciliationService.createReconciliation(
                        tenantCode,
                        new CreateReconciliationRequest(requestedDriverId, List.of(codId), null)
                )
        );
    }

    @Test
    void updateStatusReconciled_shouldUpdateLinkedCodRecords() {
        String tenantCode = "demo";
        UUID reconciliationId = UUID.randomUUID();
        ReconciliationEntity rec = reconciliation(reconciliationId, tenantCode, "OPEN");
        CodRecordEntity cod = cod(UUID.randomUUID(), UUID.randomUUID(), "COLLECTED", new BigDecimal("120000"));
        cod.setReconciliationId(reconciliationId);

        when(reconciliationRepository.findByIdAndTenantCodeAndDeletedAtIsNull(reconciliationId, tenantCode))
                .thenReturn(java.util.Optional.of(rec));
        when(codRecordRepository.findByTenantCodeAndReconciliationId(tenantCode, reconciliationId))
                .thenReturn(List.of(cod));
        when(reconciliationRepository.save(any(ReconciliationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = reconciliationService.updateReconciliationStatus(
                tenantCode,
                reconciliationId,
                new UpdateReconciliationStatusRequest("RECONCILED", "done")
        );

        assertEquals("RECONCILED", result.status());
        verify(codRecordRepository).saveAll(any());
        assertEquals("RECONCILED", cod.getStatus());
        assertNotNull(cod.getReconciledAt());
    }

    @Test
    void updateStatusCancelled_shouldUnlinkAndRollbackCodRecords() {
        String tenantCode = "demo";
        UUID reconciliationId = UUID.randomUUID();
        ReconciliationEntity rec = reconciliation(reconciliationId, tenantCode, "OPEN");
        CodRecordEntity cod = cod(UUID.randomUUID(), UUID.randomUUID(), "RECONCILED", new BigDecimal("120000"));
        cod.setReconciliationId(reconciliationId);
        cod.setReconciledAt(LocalDateTime.now());

        when(reconciliationRepository.findByIdAndTenantCodeAndDeletedAtIsNull(reconciliationId, tenantCode))
                .thenReturn(java.util.Optional.of(rec));
        when(codRecordRepository.findByTenantCodeAndReconciliationId(tenantCode, reconciliationId))
                .thenReturn(List.of(cod));
        when(reconciliationRepository.save(any(ReconciliationEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var result = reconciliationService.updateReconciliationStatus(
                tenantCode,
                reconciliationId,
                new UpdateReconciliationStatusRequest("CANCELLED", "rollback")
        );

        assertEquals("CANCELLED", result.status());
        verify(codRecordRepository).saveAll(any());
        assertEquals("COLLECTED", cod.getStatus());
        assertEquals(null, cod.getReconciliationId());
        assertEquals(null, cod.getReconciledAt());
    }

    private CodRecordEntity cod(UUID id, UUID orderId, String status, BigDecimal amount) {
        CodRecordEntity c = new CodRecordEntity();
        c.setId(id);
        c.setTenantCode("demo");
        c.setOrderId(orderId);
        c.setStatus(status);
        c.setAmount(amount);
        c.setCreatedAt(LocalDateTime.now().minusHours(1));
        return c;
    }

    private DeliveryAssignmentEntity assignment(UUID orderId, UUID driverId) {
        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantCode("demo");
        assignment.setOrderId(orderId);
        assignment.setDriverId(driverId);
        assignment.setAssignedAt(LocalDateTime.now().minusMinutes(30));
        return assignment;
    }

    private ReconciliationEntity reconciliation(UUID id, String tenantCode, String status) {
        ReconciliationEntity r = new ReconciliationEntity();
        r.setId(id);
        r.setTenantCode(tenantCode);
        r.setReconciliationCode("REC-TEST");
        r.setStatus(status);
        r.setTotalOrders(0);
        r.setTotalCodAmount(BigDecimal.ZERO);
        return r;
    }
}
