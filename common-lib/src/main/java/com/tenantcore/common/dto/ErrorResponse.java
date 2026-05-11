package com.tenantcore.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details,
        String path,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(
                code,
                message,
                null,
                path,
                LocalDateTime.now()
        );
    }

    public static ErrorResponse of(
            String code,
            String message,
            Map<String, Object> details,
            String path
    ) {
        return new ErrorResponse(
                code,
                message,
                details,
                path,
                LocalDateTime.now()
        );
    }
}