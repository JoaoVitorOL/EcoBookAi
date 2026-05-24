DROP INDEX IF EXISTS idx_revoked_jwt_token_expires_at;
DROP INDEX IF EXISTS idx_audit_log_action;
DROP INDEX IF EXISTS idx_audit_log_target_user_id;
DROP INDEX IF EXISTS idx_audit_log_actor_user_id;
DROP INDEX IF EXISTS idx_audit_log_created_at;
DROP INDEX IF EXISTS idx_consent_record_user_type;
DROP INDEX IF EXISTS idx_consent_record_user_created_at;
DROP INDEX IF EXISTS idx_solicitacao_deleted_at;
DROP INDEX IF EXISTS idx_material_upload_tracking_id;
DROP INDEX IF EXISTS idx_material_deleted_at;
DROP INDEX IF EXISTS idx_usuario_deleted_at;

DROP TABLE IF EXISTS revoked_jwt_token;
DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS consent_record;

ALTER TABLE solicitacao
    DROP COLUMN IF EXISTS anonymized,
    DROP COLUMN IF EXISTS deleted_by,
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE material
    DROP COLUMN IF EXISTS upload_tracking_id,
    DROP COLUMN IF EXISTS anonymized,
    DROP COLUMN IF EXISTS deleted_by,
    DROP COLUMN IF EXISTS deleted_at;

ALTER TABLE usuario
    DROP COLUMN IF EXISTS anonymized,
    DROP COLUMN IF EXISTS deleted_by,
    DROP COLUMN IF EXISTS deleted_at;
