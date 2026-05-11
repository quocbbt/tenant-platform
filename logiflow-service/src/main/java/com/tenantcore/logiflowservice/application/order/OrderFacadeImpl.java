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
import com.tenantcore.logiflowservice.order.OrderService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderFacadeImpl implements OrderFacade {

    private final OrderService orderService;

    public OrderFacadeImpl(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public OrderResponse createOrder(String tenantCode, CreateOrderRequest request) {
        return orderService.createOrder(tenantCode, request);
    }

    @Override
    public OrderResponse getOrder(String tenantCode, UUID id) {
        return orderService.getOrder(tenantCode, id);
    }

    @Override
    public PageResponse<OrderResponse> listOrders(String tenantCode, ListOrdersQuery query) {
        var page = orderService.listOrders(tenantCode, query);
        return PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    @Override
    public OrderResponse updateOrderStatus(String tenantCode, UUID id, UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(tenantCode, id, request);
    }

    @Override
    public SimpleActionResponse assignOrder(String tenantCode, UUID id, AssignOrderRequest request) {
        return orderService.assignOrder(tenantCode, id, request);
    }

    @Override
    public SimpleActionResponse addTrackingEvent(String tenantCode, UUID id, TrackingEventRequest request) {
        return orderService.addTrackingEvent(tenantCode, id, request);
    }

    @Override
    public SimpleActionResponse updateCod(String tenantCode, UUID id, UpdateCodRequest request) {
        return orderService.updateCod(tenantCode, id, request);
    }
}
