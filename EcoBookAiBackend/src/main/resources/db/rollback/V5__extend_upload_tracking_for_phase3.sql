DROP INDEX IF EXISTS idx_material_upload_tracking_expires_at;
DROP INDEX IF EXISTS idx_material_upload_tracking_usuario_id;

ALTER TABLE material_upload_tracking
    DROP COLUMN IF EXISTS confianca_ia,
    DROP COLUMN IF EXISTS status_ia,
    DROP COLUMN IF EXISTS expires_at,
    DROP COLUMN IF EXISTS usuario_id,
    DROP COLUMN IF EXISTS file_size,
    DROP COLUMN IF EXISTS mime_type,
    DROP COLUMN IF EXISTS file_path;
