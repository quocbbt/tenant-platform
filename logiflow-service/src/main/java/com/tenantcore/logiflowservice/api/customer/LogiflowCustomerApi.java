package com.tenantcore.logiflowservice.api.customer;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface LogiflowCustomerApi {

    ApiResponse<CustomerResponse> createCustomer(String tenantCode, @Valid CreateCustomerRequest request);

    ApiResponse<CustomerResponse> getCustomer(String tenantCode, UUID id);

    ApiResponse<PageResponse<CustomerResponse>> listCustomers(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<CustomerResponse> updateCustomer(String tenantCode, UUID id, @Valid UpdateCustomerRequest request);

    ApiResponse<String> deleteCustomer(String tenantCode, UUID id);
}
