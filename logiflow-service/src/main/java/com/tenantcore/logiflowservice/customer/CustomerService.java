package com.tenantcore.logiflowservice.customer;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.customer.dto.CreateCustomerRequest;
import com.tenantcore.logiflowservice.api.customer.dto.CustomerResponse;
import com.tenantcore.logiflowservice.api.customer.dto.ListCustomersQuery;
import com.tenantcore.logiflowservice.api.customer.dto.UpdateCustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse createCustomer(String tenantCode, CreateCustomerRequest request) {
        String customerCode = request.customerCode().trim().toUpperCase();
        if (customerRepository.existsByTenantCodeAndCustomerCodeAndDeletedAtIsNull(tenantCode, customerCode)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Customer code already exists");
        }

        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setCustomerCode(customerCode);
        entity.setCustomerName(request.customerName().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setAddress(blankToNull(request.address()));
        entity.setType(request.type() == null || request.type().isBlank() ? "NORMAL" : request.type().trim().toUpperCase());
        entity.setStatus("ACTIVE");

        return toResponse(customerRepository.save(entity));
    }

    public CustomerResponse getCustomer(String tenantCode, UUID id) {
        CustomerEntity entity = customerRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Customer not found"));
        return toResponse(entity);
    }

    public Page<CustomerResponse> listCustomers(String tenantCode, ListCustomersQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String status = blankToNull(query.status());
        String keyword = blankToNull(query.keyword());
        return customerRepository.search(tenantCode, status, keyword, pageable).map(this::toResponse);
    }

    public CustomerResponse updateCustomer(String tenantCode, UUID id, UpdateCustomerRequest request) {
        CustomerEntity entity = customerRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Customer not found"));

        entity.setCustomerName(request.customerName().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setAddress(blankToNull(request.address()));
        entity.setType(request.type() == null || request.type().isBlank() ? entity.getType() : request.type().trim().toUpperCase());
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        return toResponse(customerRepository.save(entity));
    }

    public void deleteCustomer(String tenantCode, UUID id) {
        CustomerEntity entity = customerRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Customer not found"));
        entity.setStatus("DELETED");
        entity.setDeletedAt(LocalDateTime.now());
        customerRepository.save(entity);
    }

    private CustomerResponse toResponse(CustomerEntity entity) {
        return new CustomerResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getCustomerCode(),
                entity.getCustomerName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getType(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
