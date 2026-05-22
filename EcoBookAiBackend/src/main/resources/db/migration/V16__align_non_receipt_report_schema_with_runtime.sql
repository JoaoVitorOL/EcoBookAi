DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'motivo'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN motivo TO reason;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'criado_em'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN criado_em TO created_at;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'atualizado_em'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN atualizado_em TO updated_at;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'resolvido_em'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN resolvido_em TO resolved_at;
    END IF;
END $$;

ALTER TABLE material_non_receipt_report
    ADD COLUMN IF NOT EXISTS resolution_notes VARCHAR(1000);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'status'
          AND udt_name = 'report_status_enum'
    ) THEN
        ALTER TABLE material_non_receipt_report
            ALTER COLUMN status TYPE VARCHAR(32)
            USING status::text;
    END IF;
END $$;

ALTER TABLE material_non_receipt_report
    ALTER COLUMN status SET DEFAULT 'OPEN';

DROP INDEX IF EXISTS idx_non_receipt_report_material_id;
DROP INDEX IF EXISTS idx_non_receipt_report_estudante_id;
DROP INDEX IF EXISTS idx_non_receipt_report_status;
DROP INDEX IF EXISTS idx_non_receipt_report_solicitacao_id;

ALTER TABLE material_non_receipt_report
    DROP CONSTRAINT IF EXISTS material_non_receipt_report_solicitacao_id_key;

CREATE INDEX IF NOT EXISTS idx_non_receipt_report_material_status
    ON material_non_receipt_report (material_id, status);

CREATE INDEX IF NOT EXISTS idx_non_receipt_report_student_status
    ON material_non_receipt_report (estudante_id, status);

CREATE UNIQUE INDEX IF NOT EXISTS ux_non_receipt_report_open_per_request
    ON material_non_receipt_report (solicitacao_id)
    WHERE status = 'OPEN';

DROP TYPE IF EXISTS report_status_enum;
