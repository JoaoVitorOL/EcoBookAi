DROP INDEX IF EXISTS idx_non_receipt_report_status;
DROP INDEX IF EXISTS idx_non_receipt_report_estudante_id;
DROP INDEX IF EXISTS idx_non_receipt_report_solicitacao_id;
DROP INDEX IF EXISTS idx_non_receipt_report_material_id;

DROP TABLE IF EXISTS material_non_receipt_report;
DROP TYPE IF EXISTS report_status_enum;
