package com.tenantcore.logiflowservice.web.order;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.order.LogiflowOrderApi;
import com.tenantcore.logiflowservice.api.order.dto.AssignOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.CreateOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.ListOrdersQuery;
import com.tenantcore.logiflowservice.api.order.dto.OrderResponse;
import com.tenantcore.logiflowservice.api.order.dto.SimpleActionResponse;
import com.tenantcore.logiflowservice.api.order.dto.TrackingEventRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateCodRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateOrderStatusRequest;
import com.tenantcore.logiflowservice.application.order.OrderFacade;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
public class LogiflowOrderControllerImpl implements LogiflowOrderApi {

    private final OrderFacade orderFacade;

    public LogiflowOrderControllerImpl(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @Override
    @PostMapping("/api/logiflow/orders")
    public ApiResponse<OrderResponse> createOrder(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody CreateOrderRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        OrderResponse response = orderFacade.createOrder(effectiveTenant, request);
        return ApiResponse.success("Order created", response);
    }

    @Override
    @GetMapping("/api/logiflow/orders/{id}")
    public ApiResponse<OrderResponse> getOrder(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        OrderResponse response = orderFacade.getOrder(effectiveTenant, id);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/logiflow/orders")
    public ApiResponse<PageResponse<OrderResponse>> listOrders(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        PageResponse<OrderResponse> response = orderFacade.listOrders(
                effectiveTenant,
                new ListOrdersQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @PatchMapping("/api/logiflow/orders/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        OrderResponse response = orderFacade.updateOrderStatus(effectiveTenant, id, request);
        return ApiResponse.success("Order status updated", response);
    }

    @Override
    @PostMapping("/api/logiflow/orders/{id}/assign")
    public ApiResponse<SimpleActionResponse> assignOrder(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @RequestBody AssignOrderRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        SimpleActionResponse response = orderFacade.assignOrder(effectiveTenant, id, request);
        return ApiResponse.success("Order assigned", response);
    }

    @Override
    @PostMapping("/api/logiflow/orders/{id}/tracking")
    public ApiResponse<SimpleActionResponse> addTrackingEvent(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @RequestBody TrackingEventRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        SimpleActionResponse response = orderFacade.addTrackingEvent(effectiveTenant, id, request);
        return ApiResponse.success("Tracking event added", response);
    }

    @Override
    @PostMapping("/api/logiflow/orders/{id}/cod")
    public ApiResponse<SimpleActionResponse> updateCod(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @RequestBody UpdateCodRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        SimpleActionResponse response = orderFacade.updateCod(effectiveTenant, id, request);
        return ApiResponse.success("COD updated", response);
    }

    private String resolveTenantCode(String tenantCodeHeader) {
        var currentUser = com.tenantcore.common.context.UserContext.getCurrentUser();
        if (currentUser == null || currentUser.tenantCode() == null || currentUser.tenantCode().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (tenantCodeHeader == null || tenantCodeHeader.isBlank()) {
            return currentUser.tenantCode();
        }
        if (!tenantCodeHeader.equals(currentUser.tenantCode())) {
            throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
        }
        return tenantCodeHeader;
    }
}
