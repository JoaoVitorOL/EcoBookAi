ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS anonymized BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE material
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS anonymized BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS upload_tracking_id UUID;

ALTER TABLE solicitacao
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS anonymized BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE material m
SET upload_tracking_id = tracking.id
FROM material_upload_tracking tracking
WHERE tracking.material_id = m.id
  AND m.upload_tracking_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_usuario_deleted_at
    ON usuario (deleted_at);

CREATE INDEX IF NOT EXISTS idx_material_deleted_at
    ON material (deleted_at);

CREATE INDEX IF NOT EXISTS idx_material_upload_tracking_id
    ON material (upload_tracking_id);

CREATE INDEX IF NOT EXISTS idx_solicitacao_deleted_at
    ON solicitacao (deleted_at);

CREATE TABLE IF NOT EXISTS consent_record (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    consent_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_consent_record_user_created_at
    ON consent_record (user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_consent_record_user_type
    ON consent_record (user_id, consent_type);

CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY,
    actor_user_id UUID NULL,
    actor_email VARCHAR(255) NULL,
    target_user_id UUID NULL,
    action VARCHAR(96) NOT NULL,
    resource_type VARCHAR(96) NULL,
    resource_id VARCHAR(100) NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_log_created_at
    ON audit_log (created_at);

CREATE INDEX IF NOT EXISTS idx_audit_log_actor_user_id
    ON audit_log (actor_user_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_target_user_id
    ON audit_log (target_user_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_action
    ON audit_log (action);

CREATE TABLE IF NOT EXISTS revoked_jwt_token (
    id UUID PRIMARY KEY,
    user_id UUID NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    revoked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_jwt_token_expires_at
    ON revoked_jwt_token (expires_at);
