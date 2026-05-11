CREATE TABLE logiflow_customers (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                    customer_code VARCHAR(100) NOT NULL,
                                    customer_name VARCHAR(255) NOT NULL,
                                    phone VARCHAR(30),
                                    email VARCHAR(255),
                                    address TEXT,
                                    type VARCHAR(30) DEFAULT 'NORMAL',
                                    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    created_by UUID,
                                    updated_by UUID,
                                    deleted_at TIMESTAMP,
                                    deleted_by UUID,
                                    CONSTRAINT uq_logiflow_customers_code UNIQUE (tenant_code, customer_code)
);

CREATE TABLE logiflow_drivers (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                  driver_code VARCHAR(100) NOT NULL,
                                  full_name VARCHAR(255) NOT NULL,
                                  phone VARCHAR(30),
                                  email VARCHAR(255),
                                  license_number VARCHAR(100),
                                  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  created_by UUID,
                                  updated_by UUID,
                                  deleted_at TIMESTAMP,
                                  deleted_by UUID,
                                  CONSTRAINT uq_logiflow_drivers_code UNIQUE (tenant_code, driver_code)
);

CREATE TABLE logiflow_vehicles (
                                   id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                   tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                   vehicle_code VARCHAR(100) NOT NULL,
                                   plate_number VARCHAR(50) NOT NULL,
                                   vehicle_type VARCHAR(50),
                                   capacity_kg NUMERIC(12,2),
                                   status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   created_by UUID,
                                   updated_by UUID,
                                   deleted_at TIMESTAMP,
                                   deleted_by UUID,
                                   CONSTRAINT uq_logiflow_vehicles_code UNIQUE (tenant_code, vehicle_code),
                                   CONSTRAINT uq_logiflow_vehicles_plate UNIQUE (tenant_code, plate_number)
);

CREATE TABLE logiflow_orders (
                                 id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                 order_code VARCHAR(100) NOT NULL,

                                 customer_id UUID REFERENCES logiflow_customers(id) ON DELETE SET NULL,

                                 sender_name VARCHAR(255),
                                 sender_phone VARCHAR(30),
                                 sender_address TEXT,

                                 receiver_name VARCHAR(255) NOT NULL,
                                 receiver_phone VARCHAR(30),
                                 receiver_address TEXT NOT NULL,

                                 pickup_address TEXT,
                                 delivery_address TEXT NOT NULL,

                                 goods_name VARCHAR(255),
                                 goods_note TEXT,
                                 weight_kg NUMERIC(12,2),
                                 cod_amount NUMERIC(14,2) DEFAULT 0,
                                 shipping_fee NUMERIC(14,2) DEFAULT 0,

                                 priority VARCHAR(30) DEFAULT 'NORMAL',
                                 status VARCHAR(30) NOT NULL DEFAULT 'NEW',

                                 expected_pickup_at TIMESTAMP,
                                 expected_delivery_at TIMESTAMP,
                                 completed_at TIMESTAMP,
                                 cancelled_at TIMESTAMP,
                                 cancel_reason TEXT,

                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 created_by UUID,
                                 updated_by UUID,
                                 deleted_at TIMESTAMP,
                                 deleted_by UUID,

                                 CONSTRAINT uq_logiflow_orders_code UNIQUE (tenant_code, order_code)
);

CREATE TABLE logiflow_delivery_assignments (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                               order_id UUID NOT NULL REFERENCES logiflow_orders(id) ON DELETE CASCADE,
                                               driver_id UUID REFERENCES logiflow_drivers(id) ON DELETE SET NULL,
                                               vehicle_id UUID REFERENCES logiflow_vehicles(id) ON DELETE SET NULL,
                                               status VARCHAR(30) NOT NULL DEFAULT 'ASSIGNED',
                                               assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               accepted_at TIMESTAMP,
                                               started_at TIMESTAMP,
                                               completed_at TIMESTAMP,
                                               cancelled_at TIMESTAMP,
                                               note TEXT,
                                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               created_by UUID,
                                               updated_by UUID,
                                               deleted_at TIMESTAMP,
                                               deleted_by UUID
);

CREATE TABLE logiflow_tracking_events (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                          order_id UUID NOT NULL REFERENCES logiflow_orders(id) ON DELETE CASCADE,
                                          event_code VARCHAR(100) NOT NULL,
                                          event_name VARCHAR(255) NOT NULL,
                                          description TEXT,
                                          location_text TEXT,
                                          latitude NUMERIC(10,7),
                                          longitude NUMERIC(10,7),
                                          event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          created_by UUID
);

CREATE TABLE logiflow_cod_records (
                                      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                      tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                      order_id UUID NOT NULL REFERENCES logiflow_orders(id) ON DELETE CASCADE,
                                      amount NUMERIC(14,2) NOT NULL DEFAULT 0,
                                      status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                      collected_by UUID REFERENCES logiflow_drivers(id) ON DELETE SET NULL,
                                      collected_at TIMESTAMP,
                                      reconciled_at TIMESTAMP,
                                      note TEXT,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      created_by UUID,
                                      updated_by UUID
);

CREATE TABLE logiflow_reconciliations (
                                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          tenant_code VARCHAR(50) NOT NULL REFERENCES tenants(tenant_code),
                                          reconciliation_code VARCHAR(100) NOT NULL,
                                          driver_id UUID REFERENCES logiflow_drivers(id) ON DELETE SET NULL,
                                          total_orders INT NOT NULL DEFAULT 0,
                                          total_cod_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
                                          status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                                          reconciled_at TIMESTAMP,
                                          note TEXT,
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          created_by UUID,
                                          updated_by UUID,
                                          deleted_at TIMESTAMP,
                                          deleted_by UUID,
                                          CONSTRAINT uq_logiflow_reconciliations_code UNIQUE (tenant_code, reconciliation_code)
);

CREATE INDEX idx_logiflow_customers_tenant_status
    ON logiflow_customers (tenant_code, status);

CREATE INDEX idx_logiflow_drivers_tenant_status
    ON logiflow_drivers (tenant_code, status);

CREATE INDEX idx_logiflow_vehicles_tenant_status
    ON logiflow_vehicles (tenant_code, status);

CREATE INDEX idx_logiflow_orders_tenant_status
    ON logiflow_orders (tenant_code, status, created_at DESC);

CREATE INDEX idx_logiflow_orders_customer
    ON logiflow_orders (tenant_code, customer_id);

CREATE INDEX idx_logiflow_delivery_assignments_order
    ON logiflow_delivery_assignments (tenant_code, order_id);

CREATE INDEX idx_logiflow_delivery_assignments_driver
    ON logiflow_delivery_assignments (tenant_code, driver_id, status);

CREATE INDEX idx_logiflow_tracking_events_order_time
    ON logiflow_tracking_events (tenant_code, order_id, event_time DESC);

CREATE INDEX idx_logiflow_cod_records_order
    ON logiflow_cod_records (tenant_code, order_id);

CREATE INDEX idx_logiflow_cod_records_status
    ON logiflow_cod_records (tenant_code, status);

CREATE INDEX idx_logiflow_reconciliations_driver_status
    ON logiflow_reconciliations (tenant_code, driver_id, status);

CREATE TRIGGER trg_logiflow_customers_updated_at BEFORE UPDATE ON logiflow_customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_drivers_updated_at BEFORE UPDATE ON logiflow_drivers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_vehicles_updated_at BEFORE UPDATE ON logiflow_vehicles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_orders_updated_at BEFORE UPDATE ON logiflow_orders
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_delivery_assignments_updated_at BEFORE UPDATE ON logiflow_delivery_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_cod_records_updated_at BEFORE UPDATE ON logiflow_cod_records
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_logiflow_reconciliations_updated_at BEFORE UPDATE ON logiflow_reconciliations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();