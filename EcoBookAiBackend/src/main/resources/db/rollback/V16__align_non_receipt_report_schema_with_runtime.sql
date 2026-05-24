DROP INDEX IF EXISTS ux_non_receipt_report_open_per_request;
DROP INDEX IF EXISTS idx_non_receipt_report_student_status;
DROP INDEX IF EXISTS idx_non_receipt_report_material_status;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_type
        WHERE typname = 'report_status_enum'
    ) THEN
        CREATE TYPE report_status_enum AS ENUM ('OPEN', 'RESOLVED');
    END IF;
END $$;

ALTER TABLE material_non_receipt_report
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE material_non_receipt_report
    ALTER COLUMN status TYPE report_status_enum
    USING status::report_status_enum;

ALTER TABLE material_non_receipt_report
    ALTER COLUMN status SET DEFAULT 'OPEN';

ALTER TABLE material_non_receipt_report
    DROP COLUMN IF EXISTS resolution_notes;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'reason'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN reason TO motivo;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'created_at'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN created_at TO criado_em;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'updated_at'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN updated_at TO atualizado_em;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'material_non_receipt_report'
          AND column_name = 'resolved_at'
    ) THEN
        ALTER TABLE material_non_receipt_report RENAME COLUMN resolved_at TO resolvido_em;
    END IF;
END $$;

ALTER TABLE material_non_receipt_report
    DROP CONSTRAINT IF EXISTS material_non_receipt_report_solicitacao_id_key;

ALTER TABLE material_non_receipt_report
    ADD CONSTRAINT material_non_receipt_report_solicitacao_id_key UNIQUE (solicitacao_id);

CREATE INDEX IF NOT EXISTS idx_non_receipt_report_material_id
    ON material_non_receipt_report (material_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_non_receipt_report_solicitacao_id
    ON material_non_receipt_report (solicitacao_id);

CREATE INDEX IF NOT EXISTS idx_non_receipt_report_estudante_id
    ON material_non_receipt_report (estudante_id);

CREATE INDEX IF NOT EXISTS idx_non_receipt_report_status
    ON material_non_receipt_report (status);
