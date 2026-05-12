package com.tenantcore.logiflowservice.web.vehicle;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.vehicle.LogiflowVehicleApi;
import com.tenantcore.logiflowservice.api.vehicle.dto.CreateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.ListVehiclesQuery;
import com.tenantcore.logiflowservice.api.vehicle.dto.UpdateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import com.tenantcore.logiflowservice.application.vehicle.VehicleFacade;
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
public class LogiflowVehicleControllerImpl implements LogiflowVehicleApi {

    private final VehicleFacade vehicleFacade;

    public LogiflowVehicleControllerImpl(VehicleFacade vehicleFacade) {
        this.vehicleFacade = vehicleFacade;
    }

    @Override
    @PostMapping("/api/logiflow/vehicles")
    public ApiResponse<VehicleResponse> createVehicle(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @Valid @RequestBody CreateVehicleRequest request
    ) {
        VehicleResponse response = vehicleFacade.createVehicle(resolveTenantCode(tenantCode), request);
        return ApiResponse.success("Vehicle created", response);
    }

    @Override
    @GetMapping("/api/logiflow/vehicles/{id}")
    public ApiResponse<VehicleResponse> getVehicle(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        VehicleResponse response = vehicleFacade.getVehicle(resolveTenantCode(tenantCode), id);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/logiflow/vehicles")
    public ApiResponse<PageResponse<VehicleResponse>> listVehicles(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        PageResponse<VehicleResponse> response = vehicleFacade.listVehicles(
                resolveTenantCode(tenantCode),
                new ListVehiclesQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @PutMapping("/api/logiflow/vehicles/{id}")
    public ApiResponse<VehicleResponse> updateVehicle(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request
    ) {
        VehicleResponse response = vehicleFacade.updateVehicle(resolveTenantCode(tenantCode), id, request);
        return ApiResponse.success("Vehicle updated", response);
    }

    @Override
    @DeleteMapping("/api/logiflow/vehicles/{id}")
    public ApiResponse<String> deleteVehicle(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        vehicleFacade.deleteVehicle(resolveTenantCode(tenantCode), id);
        return ApiResponse.success("Vehicle deleted", "OK");
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
