package com.tenantcore.common.exception;

public enum ErrorCode {

    SUCCESS("SUCCESS", "Success", 200),

    BAD_REQUEST("BAD_REQUEST", "Bad request", 400),
    VALIDATION_ERROR("VALIDATION_ERROR", "Validation error", 400),

    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", 401),
    INVALID_TOKEN("INVALID_TOKEN", "Invalid token", 401),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token expired", 401),

    FORBIDDEN("FORBIDDEN", "Forbidden", 403),
    TENANT_REQUIRED("TENANT_REQUIRED", "Tenant code is required", 400),
    TENANT_FORBIDDEN("TENANT_FORBIDDEN", "Tenant code is invalid or not allowed", 403),

    NOT_FOUND("NOT_FOUND", "Resource not found", 404),
    DATA_CONFLICT("DATA_CONFLICT", "Data conflict", 409),

    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", 404),
    USER_DISABLED("USER_DISABLED", "User is disabled", 403),
    INVALID_USERNAME_OR_PASSWORD("INVALID_USERNAME_OR_PASSWORD", "Invalid username or password", 401),

    ROLE_NOT_FOUND("ROLE_NOT_FOUND", "Role not found", 404),
    PERMISSION_DENIED("PERMISSION_DENIED", "Permission denied", 403),

    FILE_NOT_FOUND("FILE_NOT_FOUND", "File not found", 404),
    FILE_UPLOAD_FAILED("FILE_UPLOAD_FAILED", "File upload failed", 500),

    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Internal server error", 500);

    private final String code;
    private final String message;
    private final int httpStatus;

    ErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}