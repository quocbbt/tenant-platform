package com.tenantcore.logiflowservice.application.vehicle;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.vehicle.dto.CreateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.ListVehiclesQuery;
import com.tenantcore.logiflowservice.api.vehicle.dto.UpdateVehicleRequest;
import com.tenantcore.logiflowservice.api.vehicle.dto.VehicleResponse;
import com.tenantcore.logiflowservice.vehicle.VehicleService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class VehicleFacadeImpl implements VehicleFacade {

    private final VehicleService vehicleService;

    public VehicleFacadeImpl(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @Override
    public VehicleResponse createVehicle(String tenantCode, CreateVehicleRequest request) {
        return vehicleService.createVehicle(tenantCode, request);
    }

    @Override
    public VehicleResponse getVehicle(String tenantCode, UUID id) {
        return vehicleService.getVehicle(tenantCode, id);
    }

    @Override
    public PageResponse<VehicleResponse> listVehicles(String tenantCode, ListVehiclesQuery query) {
        var page = vehicleService.listVehicles(tenantCode, query);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public VehicleResponse updateVehicle(String tenantCode, UUID id, UpdateVehicleRequest request) {
        return vehicleService.updateVehicle(tenantCode, id, request);
    }

    @Override
    public void deleteVehicle(String tenantCode, UUID id) {
        vehicleService.deleteVehicle(tenantCode, id);
    }
}
