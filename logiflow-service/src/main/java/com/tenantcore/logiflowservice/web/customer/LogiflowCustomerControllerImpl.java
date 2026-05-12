package com.tenantcore.logiflowservice.web.customer;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.customer.LogiflowCustomerApi;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.ListCustomersQuery;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;
import com.tenantcore.logiflowservice.application.customer.CustomerFacade;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Validated
public class LogiflowCustomerControllerImpl implements LogiflowCustomerApi {

    private final CustomerFacade customerFacade;

    public LogiflowCustomerControllerImpl(CustomerFacade customerFacade) {
        this.customerFacade = customerFacade;
    }

    @Override
    @PostMapping("/api/logiflow/customers")
    public ApiResponse<CustomerResponse> createCustomer(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        CustomerResponse response = customerFacade.createCustomer(effectiveTenant, request);
        return ApiResponse.success("Customer created", response);
    }

    @Override
    @GetMapping("/api/logiflow/customers/{id}")
    public ApiResponse<CustomerResponse> getCustomer(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        CustomerResponse response = customerFacade.getCustomer(effectiveTenant, id);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/logiflow/customers")
    public ApiResponse<PageResponse<CustomerResponse>> listCustomers(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        PageResponse<CustomerResponse> response = customerFacade.listCustomers(
                effectiveTenant,
                new ListCustomersQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @PutMapping("/api/logiflow/customers/{id}")
    public ApiResponse<CustomerResponse> updateCustomer(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        CustomerResponse response = customerFacade.updateCustomer(effectiveTenant, id, request);
        return ApiResponse.success("Customer updated", response);
    }

    @Override
    @DeleteMapping("/api/logiflow/customers/{id}")
    public ApiResponse<String> deleteCustomer(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        String effectiveTenant = resolveTenantCode(tenantCode);
        customerFacade.deleteCustomer(effectiveTenant, id);
        return ApiResponse.success("Customer deleted", "OK");
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
