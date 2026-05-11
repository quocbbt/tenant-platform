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
    '11111111-1111-1111-1111-111111111111',
    'demo',
    'demo.owner',
    'demo.owner@tenantcore.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo Owner',
    'ACTIVE'
)
ON CONFLICT (tenant_code, username) DO NOTHING;

INSERT INTO user_tenants (id, user_id, tenant_code, status)
VALUES (
    gen_random_uuid(),
    '11111111-1111-1111-1111-111111111111',
    'demo',
    'ACTIVE'
)
ON CONFLICT (user_id, tenant_code) DO NOTHING;

INSERT INTO user_roles (id, tenant_code, user_id, role_id)
SELECT
    gen_random_uuid(),
    'demo',
    '11111111-1111-1111-1111-111111111111',
    r.id
FROM roles r
WHERE r.tenant_code = 'demo'
  AND r.role_code = 'OWNER'
ON CONFLICT (tenant_code, user_id, role_id) DO NOTHING;
