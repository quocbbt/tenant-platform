package com.tenantcore.logiflowservice.api.order;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.order.dto.AssignOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.CreateOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.OrderResponse;
import com.tenantcore.logiflowservice.api.order.dto.SimpleActionResponse;
import com.tenantcore.logiflowservice.api.order.dto.TrackingEventRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateOrderStatusRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateCodRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface LogiflowOrderApi {

    ApiResponse<OrderResponse> createOrder(String tenantCode, @Valid CreateOrderRequest request);

    ApiResponse<OrderResponse> getOrder(String tenantCode, UUID id);

    ApiResponse<PageResponse<OrderResponse>> listOrders(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<OrderResponse> updateOrderStatus(
            String tenantCode,
            UUID id,
            @Valid UpdateOrderStatusRequest request
    );

    ApiResponse<SimpleActionResponse> assignOrder(
            String tenantCode,
            UUID id,
            @Valid AssignOrderRequest request
    );

    ApiResponse<SimpleActionResponse> addTrackingEvent(
            String tenantCode,
            UUID id,
            @Valid TrackingEventRequest request
    );

    ApiResponse<SimpleActionResponse> updateCod(
            String tenantCode,
            UUID id,
            @Valid UpdateCodRequest request
    );
}
