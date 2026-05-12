package com.tenantcore.logiflowservice.api.driver;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.logiflowservice.api.driver.dto.CreateDriverRequest;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.driver.dto.UpdateDriverRequest;
import jakarta.validation.Valid;

import java.util.UUID;

public interface LogiflowDriverApi {

    ApiResponse<DriverResponse> createDriver(String tenantCode, @Valid CreateDriverRequest request);

    ApiResponse<DriverResponse> getDriver(String tenantCode, UUID id);

    ApiResponse<PageResponse<DriverResponse>> listDrivers(
            String tenantCode,
            int page,
            int size,
            String status,
            String keyword
    );

    ApiResponse<DriverResponse> updateDriver(String tenantCode, UUID id, @Valid UpdateDriverRequest request);

    ApiResponse<String> deleteDriver(String tenantCode, UUID id);
}
