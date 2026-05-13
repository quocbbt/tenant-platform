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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

@Tag(name = "Orders", description = "Order workflow APIs")
public interface LogiflowOrderApi {

    @Operation(summary = "Create order")
    ApiResponse<OrderResponse> createOrder(
            String tenantCode,
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"receiverName\":\"Le Thi B\",\"receiverAddress\":\"Da Nang City\",\"codAmount\":120000}")))
            @Valid CreateOrderRequest request
    );

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
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"status\":\"COMPLETED\",\"reason\":\"Delivered successfully\"}")))
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
