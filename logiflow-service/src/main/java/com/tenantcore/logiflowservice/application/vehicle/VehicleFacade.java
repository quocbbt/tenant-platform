package com.tenantcore.logiflowservice.application.vehicle;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.vehicle.dto.CreateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.ListVehiclesQuery;
import com.tenantcore.logiflowservice.api.vehicle.dto.UpdateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;

import java.util.UUID;

public interface VehicleFacade {

    VehicleResponse createVehicle(String tenantCode, CreateVehicleRequest request);

    VehicleResponse getVehicle(String tenantCode, UUID id);

    PageResponse<VehicleResponse> listVehicles(String tenantCode, ListVehiclesQuery query);

    VehicleResponse updateVehicle(String tenantCode, UUID id, UpdateVehicleRequest request);

    void deleteVehicle(String tenantCode, UUID id);
}
