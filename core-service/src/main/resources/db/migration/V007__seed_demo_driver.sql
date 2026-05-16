-- Seed demo driver user
INSERT INTO users (
    id,
    tenant_code,
    username,
    email,
    password_hash,
    full_name,
    status
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'demo',
    'demo.driver',
    'demo.driver@tenantcore.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo Driver',
    'ACTIVE'
)
ON CONFLICT (tenant_code, username) DO NOTHING;

-- Add driver to tenant
INSERT INTO user_tenants (id, user_id, tenant_code, status)
VALUES (
    gen_random_uuid(),
    '22222222-2222-2222-2222-222222222222',
    'demo',
    'ACTIVE'
)
ON CONFLICT (user_id, tenant_code) DO NOTHING;

-- Assign MEMBER role to driver
INSERT INTO user_roles (id, tenant_code, user_id, role_id)
SELECT
    gen_random_uuid(),
    'demo',
    '22222222-2222-2222-2222-222222222222',
    r.id
FROM roles r
WHERE r.tenant_code = 'demo'
  AND r.role_code = 'MEMBER'
ON CONFLICT (tenant_code, user_id, role_id) DO NOTHING;

-- Seed demo driver profile and link to user
INSERT INTO logiflow_drivers (
    id,
    tenant_code,
    driver_code,
    full_name,
    phone,
    email,
    license_number,
    status,
    user_id
)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'demo',
    'DRV-001',
    'Demo Driver',
    '0909123456',
    'demo.driver@tenantcore.local',
    'DL-2024-001',
    'ACTIVE',
    '22222222-2222-2222-2222-222222222222'
)
ON CONFLICT (tenant_code, driver_code) DO NOTHING;
