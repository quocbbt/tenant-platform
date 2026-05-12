package com.tenantcore.logiflowservice.api.vehicle;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.vehicle.dto.CreateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.UpdateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import jakarta.validation.Valid;

import java.util.UUID;

public interface LogiflowVehicleApi {

    ApiResponse<VehicleResponse> createVehicle(String tenantCode, @Valid CreateVehicleRequest request);

    ApiResponse<VehicleResponse> getVehicle(String tenantCode, UUID id);

    ApiResponse<PageResponse<VehicleResponse>> listVehicles(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<VehicleResponse> updateVehicle(String tenantCode, UUID id, @Valid UpdateVehicleRequest request);

    ApiResponse<String> deleteVehicle(String tenantCode, UUID id);
}
