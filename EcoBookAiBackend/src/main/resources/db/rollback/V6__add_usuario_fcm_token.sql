DROP INDEX IF EXISTS idx_usuario_fcm_token;

ALTER TABLE usuario
    DROP COLUMN IF EXISTS fcm_token;
