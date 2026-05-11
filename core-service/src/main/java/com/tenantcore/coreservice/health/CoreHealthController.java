package com.tenantcore.coreservice.health;

import com.tenantcore.common.dto.ApiResponse;
import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoreHealthController {

    @GetMapping("/api/core/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("core-service is running");
    }

    @GetMapping("/api/core/error-test")
    public ApiResponse<String> errorTest() {
        throw new BusinessException(ErrorCode.TENANT_FORBIDDEN);
    }
}
