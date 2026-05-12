package com.tenantcore.logiflowservice.vehicle;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.vehicle.dto.CreateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.ListVehiclesQuery;
import com.tenantcore.logiflowservice.api.vehicle.dto.UpdateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public VehicleResponse createVehicle(String tenantCode, CreateVehicleRequest request) {
        String vehicleCode = request.vehicleCode().trim().toUpperCase();
        String plateNumber = request.plateNumber().trim().toUpperCase();
        if (vehicleRepository.existsByTenantCodeAndVehicleCodeAndDeletedAtIsNull(tenantCode, vehicleCode)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Vehicle code already exists");
        }
        if (vehicleRepository.existsByTenantCodeAndPlateNumberAndDeletedAtIsNull(tenantCode, plateNumber)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Plate number already exists");
        }

        VehicleEntity entity = new VehicleEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantCode(tenantCode);
        entity.setVehicleCode(vehicleCode);
        entity.setPlateNumber(plateNumber);
        entity.setVehicleType(blankToNull(request.vehicleType()));
        entity.setCapacityKg(request.capacityKg());
        entity.setStatus("ACTIVE");
        return toResponse(vehicleRepository.save(entity));
    }

    public VehicleResponse getVehicle(String tenantCode, UUID id) {
        VehicleEntity entity = vehicleRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Vehicle not found"));
        return toResponse(entity);
    }

    public Page<VehicleResponse> listVehicles(String tenantCode, ListVehiclesQuery query) {
        int page = query.page() < 0 ? 0 : query.page();
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return vehicleRepository.search(tenantCode, blankToNull(query.status()), blankToNull(query.keyword()), pageable)
                .map(this::toResponse);
    }

    public VehicleResponse updateVehicle(String tenantCode, UUID id, UpdateVehicleRequest request) {
        VehicleEntity entity = vehicleRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Vehicle not found"));

        String plateNumber = request.plateNumber().trim().toUpperCase();
        if (!plateNumber.equals(entity.getPlateNumber())
                && vehicleRepository.existsByTenantCodeAndPlateNumberAndDeletedAtIsNull(tenantCode, plateNumber)) {
            throw new BusinessException(ErrorCode.DATA_CONFLICT, "Plate number already exists");
        }

        entity.setPlateNumber(plateNumber);
        entity.setVehicleType(blankToNull(request.vehicleType()));
        entity.setCapacityKg(request.capacityKg());
        if (request.status() != null && !request.status().isBlank()) {
            entity.setStatus(request.status().trim().toUpperCase());
        }
        return toResponse(vehicleRepository.save(entity));
    }

    public void deleteVehicle(String tenantCode, UUID id) {
        VehicleEntity entity = vehicleRepository.findByIdAndTenantCodeAndDeletedAtIsNull(id, tenantCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Vehicle not found"));
        entity.setStatus("DELETED");
        entity.setDeletedAt(LocalDateTime.now());
        vehicleRepository.save(entity);
    }

    private VehicleResponse toResponse(VehicleEntity entity) {
        return new VehicleResponse(
                entity.getId(),
                entity.getTenantCode(),
                entity.getVehicleCode(),
                entity.getPlateNumber(),
                entity.getVehicleType(),
                entity.getCapacityKg(),
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
