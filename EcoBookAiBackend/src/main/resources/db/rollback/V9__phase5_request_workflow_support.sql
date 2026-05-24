DROP INDEX IF EXISTS ux_solicitacao_material_aprovada;

ALTER TABLE solicitacao
    DROP COLUMN IF EXISTS concluido_em;

ALTER TABLE material
    DROP COLUMN IF EXISTS doado_em;
