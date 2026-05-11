package com.tenantcore.logiflowservice.health;

import com.tenantcore.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogiflowHealthController {

    @GetMapping("/api/logiflow/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("logiflow-service is running");
    }
}
