CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE tenants (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         tenant_code VARCHAR(50) NOT NULL UNIQUE,
                         name VARCHAR(255) NOT NULL,
                         domain VARCHAR(255),
                         status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                         plan VARCHAR(30) NOT NULL DEFAULT 'FREE',
                         timezone VARCHAR(100) DEFAULT 'Asia/Ho_Chi_Minh',
                         default_language VARCHAR(20) DEFAULT 'vi_VN',
                         logo_file_id UUID,
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         created_by UUID,
                         updated_by UUID,
                         deleted_at TIMESTAMP,
                         deleted_by UUID
);

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                       username VARCHAR(100) NOT NULL,
                       email VARCHAR(255),
                       phone VARCHAR(30),
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(255),
                       avatar_file_id UUID,
                       language VARCHAR(20) DEFAULT 'vi_VN',
                       timezone VARCHAR(100) DEFAULT 'Asia/Ho_Chi_Minh',
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       last_login_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       created_by UUID,
                       updated_by UUID,
                       deleted_at TIMESTAMP,
                       deleted_by UUID,
                       CONSTRAINT uq_users_tenant_username UNIQUE (tenant_code, username)
);

CREATE UNIQUE INDEX uq_users_tenant_email
    ON users (tenant_code, lower(email))
    WHERE email IS NOT NULL;

CREATE TABLE user_tenants (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                              status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                              joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT uq_user_tenants_user_tenant UNIQUE (user_id, tenant_code)
);

CREATE TABLE roles (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                       role_code VARCHAR(100) NOT NULL,
                       role_name VARCHAR(255) NOT NULL,
                       description TEXT,
                       is_system BOOLEAN NOT NULL DEFAULT FALSE,
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       created_by UUID,
                       updated_by UUID,
                       deleted_at TIMESTAMP,
                       deleted_by UUID,
                       CONSTRAINT uq_roles_tenant_code UNIQUE (tenant_code, role_code)
);

CREATE TABLE permissions (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             permission_code VARCHAR(100) NOT NULL UNIQUE,
                             permission_name VARCHAR(255) NOT NULL,
                             module_code VARCHAR(100) NOT NULL,
                             description TEXT,
                             status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                            user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                            role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            created_by UUID,
                            CONSTRAINT uq_user_roles UNIQUE (tenant_code, user_id, role_id)
);

CREATE TABLE role_permissions (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
                                  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  created_by UUID,
                                  CONSTRAINT uq_role_permissions UNIQUE (role_id, permission_id)
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                token_hash VARCHAR(255) NOT NULL UNIQUE,
                                device_id VARCHAR(255),
                                user_agent TEXT,
                                ip_address VARCHAR(100),
                                expired_at TIMESTAMP NOT NULL,
                                revoked_at TIMESTAMP,
                                status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE files (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                       original_name VARCHAR(255) NOT NULL,
                       stored_name VARCHAR(255) NOT NULL,
                       bucket VARCHAR(100) NOT NULL,
                       object_key TEXT NOT NULL,
                       content_type VARCHAR(100),
                       size_bytes BIGINT,
                       visibility VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
                       uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
                       uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at TIMESTAMP,
                       deleted_by UUID,
                       CONSTRAINT uq_files_bucket_object_key UNIQUE (bucket, object_key)
);

CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                            user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                            action VARCHAR(100) NOT NULL,
                            module_code VARCHAR(100) NOT NULL,
                            entity_type VARCHAR(100),
                            entity_id UUID,
                            old_value JSONB,
                            new_value JSONB,
                            ip_address VARCHAR(100),
                            user_agent TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                               receiver_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               title VARCHAR(255) NOT NULL,
                               content TEXT,
                               type VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
                               status VARCHAR(30) NOT NULL DEFAULT 'UNREAD',
                               read_at TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_tenant_status ON users (tenant_code, status);
CREATE INDEX idx_roles_tenant_status ON roles (tenant_code, status);
CREATE INDEX idx_user_roles_tenant_user ON user_roles (tenant_code, user_id);
CREATE INDEX idx_refresh_tokens_user_status ON refresh_tokens (tenant_code, user_id, status);
CREATE INDEX idx_files_tenant_status ON files (tenant_code, status);
CREATE INDEX idx_audit_logs_tenant_created ON audit_logs (tenant_code, created_at DESC);
CREATE INDEX idx_notifications_receiver_status
    ON notifications (tenant_code, receiver_user_id, status, created_at DESC);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_user_tenants_updated_at BEFORE UPDATE ON user_tenants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_permissions_updated_at BEFORE UPDATE ON permissions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_refresh_tokens_updated_at BEFORE UPDATE ON refresh_tokens
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_files_updated_at BEFORE UPDATE ON files
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_notifications_updated_at BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();