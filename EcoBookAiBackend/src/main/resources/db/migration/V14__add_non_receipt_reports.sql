CREATE TYPE report_status_enum AS ENUM (
    'OPEN',
    'RESOLVED'
);

CREATE TABLE material_non_receipt_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    solicitacao_id UUID NOT NULL UNIQUE REFERENCES solicitacao(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    motivo VARCHAR(500),
    status report_status_enum NOT NULL DEFAULT 'OPEN',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido_em TIMESTAMP
);

CREATE INDEX idx_non_receipt_report_material_id
    ON material_non_receipt_report (material_id);

CREATE UNIQUE INDEX idx_non_receipt_report_solicitacao_id
    ON material_non_receipt_report (solicitacao_id);

CREATE INDEX idx_non_receipt_report_estudante_id
    ON material_non_receipt_report (estudante_id);

CREATE INDEX idx_non_receipt_report_status
    ON material_non_receipt_report (status);
