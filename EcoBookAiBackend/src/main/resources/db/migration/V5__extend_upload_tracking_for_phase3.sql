ALTER TABLE material_upload_tracking
    ADD COLUMN IF NOT EXISTS file_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS mime_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS file_size BIGINT,
    ADD COLUMN IF NOT EXISTS usuario_id UUID REFERENCES usuario(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS status_ia status_ia_enum,
    ADD COLUMN IF NOT EXISTS confianca_ia NUMERIC(3, 2);

CREATE INDEX IF NOT EXISTS idx_material_upload_tracking_usuario_id
    ON material_upload_tracking(usuario_id);

CREATE INDEX IF NOT EXISTS idx_material_upload_tracking_expires_at
    ON material_upload_tracking(expires_at);
