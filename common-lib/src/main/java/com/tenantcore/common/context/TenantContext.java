package com.tenantcore.common.context;

import com.tenantcore.common.exception.BusinessException;
import com.tenantcore.common.exception.ErrorCode;

public final class TenantContext {

    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantCode(String tenantCode) {
        TENANT_CODE_HOLDER.set(tenantCode);
    }

    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    public static String requireTenantCode() {
        String tenantCode = getTenantCode();

        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_REQUIRED);
        }

        return tenantCode;
    }

    public static boolean hasTenant() {
        String tenantCode = getTenantCode();
        return tenantCode != null && !tenantCode.isBlank();
    }

    public static void clear() {
        TENANT_CODE_HOLDER.remove();
    }
}