package com.tenantcore.logiflowservice.application.customer;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.ListCustomersQuery;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;
import com.tenantcore.logiflowservice.customer.CustomerService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CustomerFacadeImpl implements CustomerFacade {

    private final CustomerService customerService;

    public CustomerFacadeImpl(CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public CustomerResponse createCustomer(String tenantCode, CreateCustomerRequest request) {
        return customerService.createCustomer(tenantCode, request);
    }

    @Override
    public CustomerResponse getCustomer(String tenantCode, UUID id) {
        return customerService.getCustomer(tenantCode, id);
    }

    @Override
    public PageResponse<CustomerResponse> listCustomers(String tenantCode, ListCustomersQuery query) {
        var page = customerService.listCustomers(tenantCode, query);
        return PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    @Override
    public CustomerResponse updateCustomer(String tenantCode, UUID id, UpdateCustomerRequest request) {
        return customerService.updateCustomer(tenantCode, id, request);
    }

    @Override
    public void deleteCustomer(String tenantCode, UUID id) {
        customerService.deleteCustomer(tenantCode, id);
    }
}
