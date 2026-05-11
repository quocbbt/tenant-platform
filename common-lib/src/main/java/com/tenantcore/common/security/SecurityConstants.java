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
}