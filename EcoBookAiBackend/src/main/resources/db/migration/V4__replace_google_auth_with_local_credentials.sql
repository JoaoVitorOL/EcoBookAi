-- Replace Google OAuth-only identity with local email/password credentials.
-- Existing Google-era accounts receive a placeholder bcrypt hash so the new
-- column can stay mandatory while preserving the rows for controlled recovery.

ALTER TABLE usuario
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

UPDATE usuario
SET password_hash = '$2a$10$zKy.OSOH8QwuPcygx3gSbeqZesX.A4MLu7YAMTLmNPfx139CWSFKW'
WHERE password_hash IS NULL;

ALTER TABLE usuario
ALTER COLUMN password_hash SET NOT NULL;

ALTER TABLE usuario
ALTER COLUMN whatsapp DROP NOT NULL,
ALTER COLUMN cidade DROP NOT NULL,
ALTER COLUMN bairro DROP NOT NULL;

DROP INDEX IF EXISTS idx_usuario_google_id;

ALTER TABLE usuario
DROP COLUMN IF EXISTS google_id;
