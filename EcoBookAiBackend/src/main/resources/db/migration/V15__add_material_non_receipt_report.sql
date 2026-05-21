CREATE TABLE material_non_receipt_report (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    solicitacao_id UUID NOT NULL REFERENCES solicitacao(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    reason VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution_notes VARCHAR(1000)
);

CREATE INDEX idx_non_receipt_report_material_status
    ON material_non_receipt_report (material_id, status);

CREATE INDEX idx_non_receipt_report_student_status
    ON material_non_receipt_report (estudante_id, status);

CREATE UNIQUE INDEX ux_non_receipt_report_open_per_request
    ON material_non_receipt_report (solicitacao_id)
    WHERE status = 'OPEN';
