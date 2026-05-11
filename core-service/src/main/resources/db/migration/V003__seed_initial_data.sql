INSERT INTO tenants (tenant_code, name, status, plan, timezone, default_language)
VALUES ('demo', 'Demo Tenant', 'ACTIVE', 'FREE', 'Asia/Ho_Chi_Minh', 'vi_VN')
    ON CONFLICT (tenant_code) DO NOTHING;

INSERT INTO permissions (permission_code, permission_name, module_code)
VALUES
    ('USER_VIEW', 'View users', 'IAM'),
    ('USER_CREATE', 'Create user', 'IAM'),
    ('USER_UPDATE', 'Update user', 'IAM'),
    ('USER_DELETE', 'Delete user', 'IAM'),

    ('ROLE_VIEW', 'View roles', 'IAM'),
    ('ROLE_CREATE', 'Create role', 'IAM'),
    ('ROLE_UPDATE', 'Update role', 'IAM'),
    ('ROLE_DELETE', 'Delete role', 'IAM'),

    ('PERMISSION_VIEW', 'View permissions', 'IAM'),

    ('FILE_UPLOAD', 'Upload file', 'FILE'),
    ('FILE_VIEW', 'View file', 'FILE'),
    ('FILE_DELETE', 'Delete file', 'FILE'),

    ('AUDIT_VIEW', 'View audit logs', 'AUDIT'),

    ('NOTIFICATION_VIEW', 'View notifications', 'NOTIFICATION'),

    ('LOGIFLOW_CUSTOMER_VIEW', 'View logistics customers', 'LOGIFLOW'),
    ('LOGIFLOW_CUSTOMER_CREATE', 'Create logistics customer', 'LOGIFLOW'),
    ('LOGIFLOW_CUSTOMER_UPDATE', 'Update logistics customer', 'LOGIFLOW'),
    ('LOGIFLOW_CUSTOMER_DELETE', 'Delete logistics customer', 'LOGIFLOW'),

    ('LOGIFLOW_DRIVER_VIEW', 'View drivers', 'LOGIFLOW'),
    ('LOGIFLOW_DRIVER_CREATE', 'Create driver', 'LOGIFLOW'),
    ('LOGIFLOW_DRIVER_UPDATE', 'Update driver', 'LOGIFLOW'),
    ('LOGIFLOW_DRIVER_DELETE', 'Delete driver', 'LOGIFLOW'),

    ('LOGIFLOW_VEHICLE_VIEW', 'View vehicles', 'LOGIFLOW'),
    ('LOGIFLOW_VEHICLE_CREATE', 'Create vehicle', 'LOGIFLOW'),
    ('LOGIFLOW_VEHICLE_UPDATE', 'Update vehicle', 'LOGIFLOW'),
    ('LOGIFLOW_VEHICLE_DELETE', 'Delete vehicle', 'LOGIFLOW'),

    ('LOGIFLOW_ORDER_VIEW', 'View logistics orders', 'LOGIFLOW'),
    ('LOGIFLOW_ORDER_CREATE', 'Create logistics order', 'LOGIFLOW'),
    ('LOGIFLOW_ORDER_UPDATE', 'Update logistics order', 'LOGIFLOW'),
    ('LOGIFLOW_ORDER_DELETE', 'Delete logistics order', 'LOGIFLOW'),
    ('LOGIFLOW_ORDER_ASSIGN', 'Assign logistics order', 'LOGIFLOW'),
    ('LOGIFLOW_ORDER_TRACKING', 'Update order tracking', 'LOGIFLOW'),

    ('LOGIFLOW_COD_VIEW', 'View COD records', 'LOGIFLOW'),
    ('LOGIFLOW_COD_UPDATE', 'Update COD records', 'LOGIFLOW'),

    ('LOGIFLOW_RECONCILIATION_VIEW', 'View reconciliations', 'LOGIFLOW'),
    ('LOGIFLOW_RECONCILIATION_CREATE', 'Create reconciliation', 'LOGIFLOW'),
    ('LOGIFLOW_RECONCILIATION_UPDATE', 'Update reconciliation', 'LOGIFLOW')
    ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO roles (tenant_code, role_code, role_name, description, is_system)
VALUES
    ('demo', 'OWNER', 'Owner', 'Full control of tenant', TRUE),
    ('demo', 'ADMIN', 'Admin', 'Tenant administrator', TRUE),
    ('demo', 'MEMBER', 'Member', 'Normal tenant member', TRUE)
    ON CONFLICT (tenant_code, role_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.tenant_code = 'demo'
  AND r.role_code = 'OWNER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN (
                                                     'USER_VIEW',
                                                     'ROLE_VIEW',
                                                     'PERMISSION_VIEW',
                                                     'FILE_UPLOAD',
                                                     'FILE_VIEW',
                                                     'AUDIT_VIEW',
                                                     'NOTIFICATION_VIEW',
                                                     'LOGIFLOW_CUSTOMER_VIEW',
                                                     'LOGIFLOW_CUSTOMER_CREATE',
                                                     'LOGIFLOW_CUSTOMER_UPDATE',
                                                     'LOGIFLOW_DRIVER_VIEW',
                                                     'LOGIFLOW_DRIVER_CREATE',
                                                     'LOGIFLOW_DRIVER_UPDATE',
                                                     'LOGIFLOW_VEHICLE_VIEW',
                                                     'LOGIFLOW_VEHICLE_CREATE',
                                                     'LOGIFLOW_VEHICLE_UPDATE',
                                                     'LOGIFLOW_ORDER_VIEW',
                                                     'LOGIFLOW_ORDER_CREATE',
                                                     'LOGIFLOW_ORDER_UPDATE',
                                                     'LOGIFLOW_ORDER_ASSIGN',
                                                     'LOGIFLOW_ORDER_TRACKING',
                                                     'LOGIFLOW_COD_VIEW',
                                                     'LOGIFLOW_COD_UPDATE',
                                                     'LOGIFLOW_RECONCILIATION_VIEW',
                                                     'LOGIFLOW_RECONCILIATION_CREATE',
                                                     'LOGIFLOW_RECONCILIATION_UPDATE'
    )
WHERE r.tenant_code = 'demo'
  AND r.role_code = 'ADMIN'
    ON CONFLICT (role_id, permission_id) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.permission_code IN (
                                                     'FILE_UPLOAD',
                                                     'FILE_VIEW',
                                                     'NOTIFICATION_VIEW',
                                                     'LOGIFLOW_CUSTOMER_VIEW',
                                                     'LOGIFLOW_DRIVER_VIEW',
                                                     'LOGIFLOW_VEHICLE_VIEW',
                                                     'LOGIFLOW_ORDER_VIEW',
                                                     'LOGIFLOW_ORDER_CREATE',
                                                     'LOGIFLOW_ORDER_UPDATE',
                                                     'LOGIFLOW_ORDER_TRACKING',
                                                     'LOGIFLOW_COD_VIEW'
    )
WHERE r.tenant_code = 'demo'
  AND r.role_code = 'MEMBER'
    ON CONFLICT (role_id, permission_id) DO NOTHING;