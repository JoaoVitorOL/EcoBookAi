ALTER TABLE material
    ADD COLUMN IF NOT EXISTS doado_em TIMESTAMP;

ALTER TABLE solicitacao
    ADD COLUMN IF NOT EXISTS concluido_em TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS ux_solicitacao_material_aprovada
    ON solicitacao (material_id)
    WHERE status = 'APROVADA';
