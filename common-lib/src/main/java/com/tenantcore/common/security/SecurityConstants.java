package com.tenantcore.common.security;

public final class SecurityConstants {

    private SecurityConstants() {
    }

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String TENANT_HEADER = "X-Tenant-Code";

    public static final String USER_ID_CLAIM = "userId";
    public static final String TENANT_CODE_CLAIM = "tenantCode";
    public static final String USERNAME_CLAIM = "username";
    public static final String ROLES_CLAIM = "roles";
    public static final String PERMISSIONS_CLAIM = "permissions";

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MEMBER = "MEMBER";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";
    public static final String STATUS_DELETED = "DELETED";

    public static final String PERMISSION_USER_VIEW = "USER_VIEW";
    public static final String PERMISSION_USER_CREATE = "USER_CREATE";
    public static final String PERMISSION_USER_UPDATE = "USER_UPDATE";
    public static final String PERMISSION_USER_DELETE = "USER_DELETE";

    public static final String PERMISSION_ROLE_VIEW = "ROLE_VIEW";
    public static final String PERMISSION_ROLE_CREATE = "ROLE_CREATE";
    public static final String PERMISSION_ROLE_UPDATE = "ROLE_UPDATE";
    public static final String PERMISSION_ROLE_DELETE = "ROLE_DELETE";

    public static final String PERMISSION_LOGIFLOW_ORDER_VIEW = "LOGIFLOW_ORDER_VIEW";
    public static final String PERMISSION_LOGIFLOW_ORDER_CREATE = "LOGIFLOW_ORDER_CREATE";
    public static final String PERMISSION_LOGIFLOW_ORDER_UPDATE = "LOGIFLOW_ORDER_UPDATE";
    public static final String PERMISSION_LOGIFLOW_ORDER_DELETE = "LOGIFLOW_ORDER_DELETE";
    public static final String PERMISSION_LOGIFLOW_ORDER_ASSIGN = "LOGIFLOW_ORDER_ASSIGN";
    public static final String PERMISSION_LOGIFLOW_ORDER_TRACKING = "LOGIFLOW_ORDER_TRACKING";

    public static final String PERMISSION_LOGIFLOW_CUSTOMER_VIEW = "LOGIFLOW_CUSTOMER_VIEW";
    public static final String PERMISSION_LOGIFLOW_CUSTOMER_CREATE = "LOGIFLOW_CUSTOMER_CREATE";
    public static final String PERMISSION_LOGIFLOW_CUSTOMER_UPDATE = "LOGIFLOW_CUSTOMER_UPDATE";
    public static final String PERMISSION_LOGIFLOW_CUSTOMER_DELETE = "LOGIFLOW_CUSTOMER_DELETE";

    public static final String PERMISSION_LOGIFLOW_DRIVER_VIEW = "LOGIFLOW_DRIVER_VIEW";
    public static final String PERMISSION_LOGIFLOW_DRIVER_CREATE = "LOGIFLOW_DRIVER_CREATE";
    public static final String PERMISSION_LOGIFLOW_DRIVER_UPDATE = "LOGIFLOW_DRIVER_UPDATE";
    public static final String PERMISSION_LOGIFLOW_DRIVER_DELETE = "LOGIFLOW_DRIVER_DELETE";

    public static final String PERMISSION_LOGIFLOW_VEHICLE_VIEW = "LOGIFLOW_VEHICLE_VIEW";
    public static final String PERMISSION_LOGIFLOW_VEHICLE_CREATE = "LOGIFLOW_VEHICLE_CREATE";
    public static final String PERMISSION_LOGIFLOW_VEHICLE_UPDATE = "LOGIFLOW_VEHICLE_UPDATE";
    public static final String PERMISSION_LOGIFLOW_VEHICLE_DELETE = "LOGIFLOW_VEHICLE_DELETE";

    public static final String PERMISSION_LOGIFLOW_COD_VIEW = "LOGIFLOW_COD_VIEW";
    public static final String PERMISSION_LOGIFLOW_COD_UPDATE = "LOGIFLOW_COD_UPDATE";

    public static final String PERMISSION_LOGIFLOW_RECONCILIATION_VIEW = "LOGIFLOW_RECONCILIATION_VIEW";
    public static final String PERMISSION_LOGIFLOW_RECONCILIATION_CREATE = "LOGIFLOW_RECONCILIATION_CREATE";
    public static final String PERMISSION_LOGIFLOW_RECONCILIATION_UPDATE = "LOGIFLOW_RECONCILIATION_UPDATE";

    public static final String PERMISSION_NOTIFICATION_VIEW = "NOTIFICATION_VIEW";
}
