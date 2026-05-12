package com.tenantcore.logiflowservice.driver;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.driver.dto.CreateDriverRequest;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.driver.dto.ListDriversQuery;
import com.tenantcore.logiflowservice.api.driver.dto.UpdateDriverRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public DriverResponse createDriver(String tenantCode, CreateDriverRequest request) {
        String driverCode = request.driverCode().trim().toUpperCase();
        if (driverRepository.existsByTenantCodeAndDriverCodeAndDeletedAtIsNull(tenantCode, driverCode)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Driver code already exists");
        }

        DriverEntity entity = new DriverEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setDriverCode(driverCode);
        entity.setFullName(request.fullName().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setLicenseNumber(blankToNull(request.licenseNumber()));
        entity.setStatus("ACTIVE");
        return toResponse(driverRepository.save(entity));
    }

    public DriverResponse getDriver(String tenantCode, UUID id) {
        DriverEntity entity = driverRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Driver not found"));
        return toResponse(entity);
    }

    public Page<DriverResponse> listDrivers(String tenantCode, ListDriversQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return driverRepository.search(tenantCode, blankToNull(query.status()), blankToNull(query.keyword()), pageable)
                .map(this::toResponse);
    }

    public DriverResponse updateDriver(String tenantCode, UUID id, UpdateDriverRequest request) {
        DriverEntity entity = driverRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Driver not found"));

        entity.setFullName(request.fullName().trim());
        entity.setPhone(blankToNull(request.phone()));
        entity.setEmail(blankToNull(request.email()));
        entity.setLicenseNumber(blankToNull(request.licenseNumber()));
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        return toResponse(driverRepository.save(entity));
    }

    public void deleteDriver(String tenantCode, UUID id) {
        DriverEntity entity = driverRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Driver not found"));
        entity.setStatus("DELETED");
        entity.setDeletedAt(LocalDateTime.now());
        driverRepository.save(entity);
    }

    private DriverResponse toResponse(DriverEntity entity) {
        return new DriverResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getDriverCode(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getLicenseNumber(),
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
