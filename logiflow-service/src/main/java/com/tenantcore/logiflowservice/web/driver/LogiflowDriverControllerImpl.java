package com.tenantcore.logiflowservice.web.driver;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.dto.PageResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import com.tenantcore.logiflowservice.api.driver.LogiflowDriverApi;
import com.tenantcore.logiflowservice.api.driver.dto.CreateDriverRequest;
import com.tenantcore.logiflowservice.api.driver.dto.DriverResponse;
import com.tenantcore.logiflowservice.api.driver.dto.ListDriversQuery;
import com.tenantcore.logiflowservice.api.driver.dto.UpdateDriverRequest;
import com.tenantcore.logiflowservice.application.driver.DriverFacade;
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
public class LogiflowDriverControllerImpl implements LogiflowDriverApi {

    private final DriverFacade driverFacade;

    public LogiflowDriverControllerImpl(DriverFacade driverFacade) {
        this.driverFacade = driverFacade;
    }

    @Override
    @PostMapping("/api/logiflow/drivers")
    public ApiResponse<DriverResponse> createDriver(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @Valid @RequestBody CreateDriverRequest request
    ) {
        DriverResponse response = driverFacade.createDriver(resolveTenantCode(tenantCode), request);
        return ApiResponse.success("Driver created", response);
    }

    @Override
    @GetMapping("/api/logiflow/drivers/{id}")
    public ApiResponse<DriverResponse> getDriver(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        DriverResponse response = driverFacade.getDriver(resolveTenantCode(tenantCode), id);
        return ApiResponse.success(response);
    }

    @Override
    @GetMapping("/api/logiflow/drivers")
    public ApiResponse<PageResponse<DriverResponse>> listDrivers(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        PageResponse<DriverResponse> response = driverFacade.listDrivers(
                resolveTenantCode(tenantCode),
                new ListDriversQuery(page, size, status, keyword)
        );
        return ApiResponse.success(response);
    }

    @Override
    @PutMapping("/api/logiflow/drivers/{id}")
    public ApiResponse<DriverResponse> updateDriver(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDriverRequest request
    ) {
        DriverResponse response = driverFacade.updateDriver(resolveTenantCode(tenantCode), id, request);
        return ApiResponse.success("Driver updated", response);
    }

    @Override
    @DeleteMapping("/api/logiflow/drivers/{id}")
    public ApiResponse<String> deleteDriver(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @PathVariable UUID id
    ) {
        driverFacade.deleteDriver(resolveTenantCode(tenantCode), id);
        return ApiResponse.success("Driver deleted", "OK");
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
