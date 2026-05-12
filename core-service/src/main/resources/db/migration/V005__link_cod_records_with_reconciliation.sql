ALTER TABLE logiflow_cod_records
    ADD COLUMN IF NOT EXISTS reconciliation_id UUID;

ALTER TABLE logiflow_cod_records
    ADD CONSTRAINT fk_logiflow_cod_records_reconciliation
        FOREIGN KEY (reconciliation_id) REFERENCES logiflow_reconciliations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_logiflow_cod_records_reconciliation
    ON logiflow_cod_records (tenant_code, reconciliation_id);
