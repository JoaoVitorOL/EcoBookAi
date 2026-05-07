ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS fcm_token VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_usuario_fcm_token
    ON usuario (fcm_token);
