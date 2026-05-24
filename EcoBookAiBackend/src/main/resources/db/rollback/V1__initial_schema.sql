DROP VIEW IF EXISTS view_active_requests;

DROP TRIGGER IF EXISTS trigger_solicitacao_atualizado_em ON solicitacao;
DROP TRIGGER IF EXISTS trigger_material_atualizado_em ON material;
DROP TRIGGER IF EXISTS trigger_usuario_atualizado_em ON usuario;

DROP FUNCTION IF EXISTS update_atualizado_em();

DROP TABLE IF EXISTS material_upload_tracking CASCADE;
DROP TABLE IF EXISTS solicitacao CASCADE;
DROP TABLE IF EXISTS material CASCADE;
DROP TABLE IF EXISTS usuario_necessidades CASCADE;
DROP TABLE IF EXISTS usuario CASCADE;

DROP TYPE IF EXISTS necessidade_academica_enum;
DROP TYPE IF EXISTS role_enum;
DROP TYPE IF EXISTS status_ia_enum;
DROP TYPE IF EXISTS status_solicitacao_enum;
DROP TYPE IF EXISTS status_material_enum;
DROP TYPE IF EXISTS estado_conservacao_enum;
DROP TYPE IF EXISTS sistema_ensino_enum;
DROP TYPE IF EXISTS nivel_ensino_enum;
DROP TYPE IF EXISTS disciplina_enum;
