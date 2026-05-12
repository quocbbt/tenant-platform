package com.tenantcore.logiflowservice.application.driver;

import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.driver.dto.CreateDriverRequest;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.driver.dto.ListDriversQuery;
import com.tenantcore.logiflowservice.api.driver.dto.UpdateDriverRequest;

import java.util.UUID;

public interface DriverFacade {

    DriverResponse createDriver(String tenantCode, CreateDriverRequest request);

    DriverResponse getDriver(String tenantCode, UUID id);

    PageResponse<DriverResponse> listDrivers(String tenantCode, ListDriversQuery query);

    DriverResponse updateDriver(String tenantCode, UUID id, UpdateDriverRequest request);

    void deleteDriver(String tenantCode, UUID id);
}
