package com.tenantcore.logiflowservice.api.customer;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

@Tag(name = "Customers", description = "Customer management APIs")
public interface LogiflowCustomerApi {

    @Operation(summary = "Create customer")
    ApiResponse<CustomerResponse> createCustomer(
            String tenantCode,
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"customerCode\":\"CUS-001\",\"customerName\":\"Nguyen Van A\",\"phone\":\"0909123456\",\"email\":\"customer@example.com\",\"address\":\"District 1, Ho Chi Minh City\",\"type\":\"NORMAL\"}")))
            @Valid CreateCustomerRequest request
    );

    ApiResponse<CustomerResponse> getCustomer(String tenantCode, UUID id);

    ApiResponse<PageResponse<CustomerResponse>> listCustomers(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    @Operation(summary = "Update customer")
    ApiResponse<CustomerResponse> updateCustomer(
            String tenantCode,
            UUID id,
            @RequestBody(
                    required = true,
                    content = @Content(examples = @ExampleObject(value = "{\"customerName\":\"Nguyen Van B\",\"phone\":\"0909888777\",\"email\":\"customer.updated@example.com\",\"address\":\"District 7, Ho Chi Minh City\",\"type\":\"VIP\",\"status\":\"ACTIVE\"}")))
            @Valid UpdateCustomerRequest request
    );

    ApiResponse<String> deleteCustomer(String tenantCode, UUID id);
}
