package com.tenantcore.logiflowservice.order;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.order.dto.ListOrdersQuery;
import com.tenantcore.logiflowservice.api.order.dto.CreateOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.OrderResponse;
import com.tenantcore.logiflowservice.api.order.dto.AssignOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.SimpleActionResponse;
import com.tenantcore.logiflowservice.api.order.dto.TrackingEventRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateCodRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateOrderStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final DeliveryAssignmentRepository deliveryAssignmentRepository;
    private final TrackingEventRepository trackingEventRepository;
    private final CodRecordRepository codRecordRepository;

    public OrderService(
            OrderRepository orderRepository,
            DeliveryAssignmentRepository deliveryAssignmentRepository,
            TrackingEventRepository trackingEventRepository,
            CodRecordRepository codRecordRepository
    ) {
        this.orderRepository = orderRepository;
        this.deliveryAssignmentRepository = deliveryAssignmentRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.codRecordRepository = codRecordRepository;
    }

    public OrderResponse createOrder(String tenantCode, CreateOrderRequest request) {
        OrderEntity entity = new OrderEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setOrderCode("ORD-" + System.currentTimeMillis());
        entity.setReceiverName(request.receiverName());
        entity.setReceiverAddress(request.receiverAddress());
        entity.setDeliveryAddress(request.receiverAddress());
        entity.setCodAmount(request.codAmount() == null ? BigDecimal.ZERO : request.codAmount());
        entity.setStatus("NEW");

        OrderEntity saved = orderRepository.save(entity);
        return toResponse(saved);
    }

    public OrderResponse getOrder(String tenantCode, UUID id) {
        OrderEntity entity = orderRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));
        return toResponse(entity);
    }

    public Page<OrderResponse> listOrders(String tenantCode, ListOrdersQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String status = blankToNull(query.status());
        String keyword = blankToNull(query.keyword());
        Page<OrderEntity> entityPage;
        if (status == null && keyword == null) {
            entityPage = orderRepository.findByTenantCode(tenantCode, pageable);
        } else if (status != null && keyword == null) {
            entityPage = orderRepository.findByTenantCodeAndStatus(tenantCode, status, pageable);
        } else if (status == null) {
            entityPage = orderRepository.searchByTenantAndKeyword(tenantCode, keyword, pageable);
        } else {
            entityPage = orderRepository.searchByTenantAndStatusAndKeyword(tenantCode, status, keyword, pageable);
        }
        return entityPage.map(this::toResponse);
    }

    public OrderResponse updateOrderStatus(String tenantCode, UUID id, UpdateOrderStatusRequest request) {
        OrderEntity entity = orderRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        String newStatus = normalizeStatus(request.status());
        entity.setStatus(newStatus);
        if ("COMPLETED".equals(newStatus)) {
            entity.setCompletedAt(LocalDateTime.now());
            entity.setCancelledAt(null);
            entity.setCancelReason(null);
        } else if ("CANCELLED".equals(newStatus)) {
            entity.setCancelledAt(LocalDateTime.now());
            entity.setCancelReason(blankToNull(request.reason()));
            entity.setCompletedAt(null);
        } else {
            entity.setCompletedAt(null);
            entity.setCancelledAt(null);
            entity.setCancelReason(null);
        }

        OrderEntity saved = orderRepository.save(entity);
        return toResponse(saved);
    }

    public SimpleActionResponse assignOrder(String tenantCode, UUID id, AssignOrderRequest request) {
        OrderEntity order = orderRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        DeliveryAssignmentEntity assignment = new DeliveryAssignmentEntity();
        assignment.setId(UUID.randomUUID());
        assignment.setTenantCode(tenantCode);
        assignment.setOrderId(order.getId());
        assignment.setDriverId(request.driverId());
        assignment.setVehicleId(request.vehicleId());
        assignment.setStatus("ASSIGNED");
        assignment.setNote(blankToNull(request.note()));
        deliveryAssignmentRepository.save(assignment);

        order.setStatus("ASSIGNED");
        orderRepository.save(order);
        return new SimpleActionResponse("ASSIGN_ORDER", "SUCCESS");
    }

    public SimpleActionResponse addTrackingEvent(String tenantCode, UUID id, TrackingEventRequest request) {
        OrderEntity order = orderRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        TrackingEventEntity event = new TrackingEventEntity();
        event.setId(UUID.randomUUID());
        event.setTenantCode(tenantCode);
        event.setOrderId(order.getId());
        event.setEventCode(request.eventCode().trim().toUpperCase());
        event.setEventName(request.eventName().trim());
        event.setDescription(blankToNull(request.description()));
        event.setLocationText(blankToNull(request.locationText()));
        event.setLatitude(request.latitude());
        event.setLongitude(request.longitude());
        event.setEventTime(request.eventTime() == null ? LocalDateTime.now() : request.eventTime());
        trackingEventRepository.save(event);

        if ("DELIVERED".equals(event.getEventCode())) {
            order.setStatus("COMPLETED");
            order.setCompletedAt(LocalDateTime.now());
            orderRepository.save(order);
        } else if ("IN_TRANSIT".equals(event.getEventCode())) {
            order.setStatus("IN_TRANSIT");
            orderRepository.save(order);
        }
        return new SimpleActionResponse("ADD_TRACKING_EVENT", "SUCCESS");
    }

    public SimpleActionResponse updateCod(String tenantCode, UUID id, UpdateCodRequest request) {
        OrderEntity order = orderRepository.findByIdAndTenantCode(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        CodRecordEntity cod = new CodRecordEntity();
        cod.setId(UUID.randomUUID());
        cod.setTenantCode(tenantCode);
        cod.setOrderId(order.getId());
        cod.setAmount(request.amount());
        cod.setStatus(request.status() == null || request.status().isBlank() ? "PENDING" : request.status().trim().toUpperCase());
        cod.setNote(blankToNull(request.note()));
        codRecordRepository.save(cod);
        return new SimpleActionResponse("UPDATE_COD", "SUCCESS");
    }

    private OrderResponse toResponse(OrderEntity entity) {
        LocalDateTime createdAt = entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt();
        return new OrderResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getOrderCode(),
                entity.getReceiverName(),
                entity.getReceiverAddress(),
                entity.getCodAmount(),
                entity.getStatus(),
                createdAt
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
            case "NEW", "ASSIGNED", "IN_TRANSIT", "COMPLETED", "CANCELLED" -> normalized;
            default -> throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported order status: " + status);
        };
    }
}
