package com.tenantcore.logiflowservice.application.customer;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.ListCustomersQuery;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;

import java.util.UUID;

public interface CustomerFacade {

    CustomerResponse createCustomer(String tenantCode, CreateCustomerRequest request);

    CustomerResponse getCustomer(String tenantCode, UUID id);

    PageResponse<CustomerResponse> listCustomers(String tenantCode, ListCustomersQuery query);

    CustomerResponse updateCustomer(String tenantCode, UUID id, UpdateCustomerRequest request);

    void deleteCustomer(String tenantCode, UUID id);
}
