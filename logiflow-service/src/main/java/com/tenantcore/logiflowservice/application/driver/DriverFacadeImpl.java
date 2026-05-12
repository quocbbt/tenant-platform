package com.tenantcore.logiflowservice.application.driver;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.driver.dto.CreateDriverRequest;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.driver.dto.ListDriversQuery;
import com.tenantcore.logiflowservice.api.driver.dto.UpdateDriverRequest;
import com.tenantcore.logiflowservice.driver.DriverService;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DriverFacadeImpl implements DriverFacade {

    private final DriverService driverService;

    public DriverFacadeImpl(DriverService driverService) {
        this.driverService = driverService;
    }

    @Override
    public DriverResponse createDriver(String tenantCode, CreateDriverRequest request) {
        return driverService.createDriver(tenantCode, request);
    }

    @Override
    public DriverResponse getDriver(String tenantCode, UUID id) {
        return driverService.getDriver(tenantCode, id);
    }

    @Override
    public PageResponse<DriverResponse> listDrivers(String tenantCode, ListDriversQuery query) {
        var page = driverService.listDrivers(tenantCode, query);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    public DriverResponse updateDriver(String tenantCode, UUID id, UpdateDriverRequest request) {
        return driverService.updateDriver(tenantCode, id, request);
    }

    @Override
    public void deleteDriver(String tenantCode, UUID id) {
        driverService.deleteDriver(tenantCode, id);
    }
}
