package com.tenantcore.logiflowservice.application.order;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.order.dto.AssignOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.CreateOrderRequest;
import com.tenantcore.logiflowservice.api.order.dto.ListOrdersQuery;
import com.tenantcore.logiflowservice.api.order.dto.OrderResponse;
import com.tenantcore.logiflowservice.api.order.dto.SimpleActionResponse;
import com.tenantcore.logiflowservice.api.order.dto.TrackingEventRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateCodRequest;
import com.tenantcore.logiflowservice.api.order.dto.UpdateOrderStatusRequest;

import java.util.UUID;

public interface OrderFacade {

    OrderResponse createOrder(String tenantCode, CreateOrderRequest request);

    OrderResponse getOrder(String tenantCode, UUID id);

    PageResponse<OrderResponse> listOrders(String tenantCode, ListOrdersQuery query);

    OrderResponse updateOrderStatus(String tenantCode, UUID id, UpdateOrderStatusRequest request);

    SimpleActionResponse assignOrder(String tenantCode, UUID id, AssignOrderRequest request);

    SimpleActionResponse addTrackingEvent(String tenantCode, UUID id, TrackingEventRequest request);

    SimpleActionResponse updateCod(String tenantCode, UUID id, UpdateCodRequest request);
}
