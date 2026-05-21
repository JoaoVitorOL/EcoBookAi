-- EcoBook IA - Convenience PostgreSQL Schema Snapshot
-- Version: 2.2
-- Date: 2026-05-21
-- Source of truth: Flyway migrations V1 through V15 plus current JPA mappings
-- If any conflict exists, the migrations and the JPA model win over this snapshot.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE disciplina_enum AS ENUM (
    'TODAS',
    'MATEMATICA',
    'PORTUGUES',
    'HISTORIA',
    'GEOGRAFIA',
    'CIENCIAS',
    'LITERATURA'
);

CREATE TYPE nivel_ensino_enum AS ENUM (
    'FUNDAMENTAL',
    'MEDIO',
    'SUPERIOR'
);

CREATE TYPE sistema_ensino_enum AS ENUM (
    'ANGLO',
    'OBJETIVO',
    'COC',
    'POSITIVO',
    'POLIEDRO',
    'ETAPA',
    'BERNOULLI',
    'SAS',
    'FTD',
    'OUTRO'
);

CREATE TYPE estado_conservacao_enum AS ENUM (
    'NOVO',
    'BOM',
    'USADO',
    'DANIFICADO'
);

CREATE TYPE status_material_enum AS ENUM (
    'DISPONIVEL',
    'RESERVADO',
    'DOADO',
    'CANCELADO'
);

CREATE TYPE status_solicitacao_enum AS ENUM (
    'PENDENTE',
    'APROVADA',
    'RECUSADA',
    'CANCELADA',
    'CONCLUIDA'
);

CREATE TYPE status_ia_enum AS ENUM (
    'SUCCESS',
    'LOW_CONFIDENCE',
    'FAILURE',
    'NOT_ATTEMPTED'
);

CREATE TYPE role_enum AS ENUM (
    'USER',
    'ADMIN'
);

CREATE TYPE necessidade_academica_enum AS ENUM (
    'TEXTBOOKS',
    'WORKBOOKS',
    'REFERENCE_MATERIALS',
    'FICTION',
    'TECHNICAL_BOOKS',
    'TEST_PREP'
);

-- ============================================================================
-- TABLES
-- ============================================================================

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    whatsapp VARCHAR(20),
    cidade VARCHAR(100),
    bairro VARCHAR(100),
    instituicao VARCHAR(255),
    fcm_token VARCHAR(512),
    perfil_completo BOOLEAN NOT NULL DEFAULT false,
    consentimento_ia BOOLEAN NOT NULL DEFAULT false,
    role role_enum NOT NULL DEFAULT 'USER',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_necessidades (
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    necessidade necessidade_academica_enum NOT NULL,
    PRIMARY KEY (usuario_id, necessidade)
);

CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doador_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    titulo VARCHAR(255) NOT NULL,
    autor VARCHAR(255),
    editora VARCHAR(255),
    descricao TEXT,
    disciplina disciplina_enum NOT NULL,
    nivel_ensino nivel_ensino_enum NOT NULL,
    ano INTEGER,
    sistema_ensino sistema_ensino_enum NOT NULL,
    estado_conservacao estado_conservacao_enum NOT NULL,
    status status_material_enum NOT NULL DEFAULT 'DISPONIVEL',
    imagem_url VARCHAR(500),
    imagem_verso_url VARCHAR(500),
    upload_id VARCHAR(100),
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    data_publicacao INTEGER,
    status_ia status_ia_enum,
    confianca_ia DECIMAL(3, 2),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    doado_em TIMESTAMP
);

CREATE TABLE solicitacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    status status_solicitacao_enum NOT NULL DEFAULT 'PENDENTE',
    contato_doador JSONB,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aprovado_em TIMESTAMP,
    expires_at TIMESTAMP,
    concluido_em TIMESTAMP
);

CREATE TABLE material_upload_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id VARCHAR(100) NOT NULL UNIQUE,
    material_id UUID REFERENCES material(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL,
    usuario_id UUID REFERENCES usuario(id) ON DELETE CASCADE,
    file_path VARCHAR(1000),
    secondary_file_path VARCHAR(1000),
    mime_type VARCHAR(100),
    secondary_mime_type VARCHAR(100),
    file_size BIGINT,
    secondary_file_size BIGINT,
    expires_at TIMESTAMP,
    status_ia status_ia_enum,
    confianca_ia DECIMAL(3, 2),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    notification_id VARCHAR(100) NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(512) NOT NULL,
    route VARCHAR(120) NOT NULL,
    request_id UUID,
    material_id UUID,
    payload_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP
);

CREATE TABLE failed_notification (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(512) NOT NULL,
    payload_data JSONB NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_attempt_at TIMESTAMP NOT NULL,
    last_attempt_at TIMESTAMP,
    delivered_at TIMESTAMP,
    permanently_failed_at TIMESTAMP,
    last_error VARCHAR(1000)
);

CREATE TABLE material_non_receipt_report (
    id UUID PRIMARY KEY,
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    solicitacao_id UUID NOT NULL REFERENCES solicitacao(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    reason VARCHAR(500),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution_notes VARCHAR(1000)
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_perfil_completo ON usuario(perfil_completo);
CREATE INDEX idx_usuario_fcm_token ON usuario(fcm_token);

CREATE INDEX idx_material_status ON material(status);
CREATE INDEX idx_material_status_disciplina ON material(status, disciplina);
CREATE INDEX idx_material_status_nivel_ensino ON material(status, nivel_ensino);
CREATE INDEX idx_material_sistema_ensino ON material(sistema_ensino);
CREATE INDEX idx_material_cidade_bairro ON material(cidade, bairro);
CREATE INDEX idx_material_data_publicacao ON material(data_publicacao DESC);
CREATE INDEX idx_material_doador_id ON material(doador_id);

CREATE INDEX idx_solicitacao_material_id ON solicitacao(material_id);
CREATE INDEX idx_solicitacao_estudante_id ON solicitacao(estudante_id);
CREATE INDEX idx_solicitacao_status ON solicitacao(status);
CREATE INDEX idx_solicitacao_expires_at ON solicitacao(expires_at);

CREATE UNIQUE INDEX ux_solicitacao_material_aprovada
    ON solicitacao (material_id)
    WHERE status = 'APROVADA';

CREATE INDEX idx_material_upload_tracking_upload_id ON material_upload_tracking(upload_id);
CREATE INDEX idx_material_upload_tracking_status ON material_upload_tracking(status);
CREATE INDEX idx_material_upload_tracking_usuario_id ON material_upload_tracking(usuario_id);
CREATE INDEX idx_material_upload_tracking_expires_at ON material_upload_tracking(expires_at);

CREATE UNIQUE INDEX uk_user_notification_user_notification_id
    ON user_notification (user_id, notification_id);

CREATE INDEX idx_user_notification_user_created_at
    ON user_notification (user_id, created_at DESC);

CREATE INDEX idx_user_notification_user_read_at
    ON user_notification (user_id, read_at);

CREATE INDEX idx_failed_notification_next_attempt
    ON failed_notification (next_attempt_at);

CREATE INDEX idx_failed_notification_user_id
    ON failed_notification (user_id);

CREATE INDEX idx_non_receipt_report_material_status
    ON material_non_receipt_report (material_id, status);

CREATE INDEX idx_non_receipt_report_student_status
    ON material_non_receipt_report (estudante_id, status);

CREATE UNIQUE INDEX ux_non_receipt_report_open_per_request
    ON material_non_receipt_report (solicitacao_id)
    WHERE status = 'OPEN';

-- ============================================================================
-- CONSTRAINTS AND HELPERS
-- ============================================================================

ALTER TABLE material
ADD CONSTRAINT check_ano_with_nivel
CHECK (
    (nivel_ensino = 'FUNDAMENTAL' AND ano BETWEEN 1 AND 9) OR
    (nivel_ensino = 'MEDIO' AND ano BETWEEN 1 AND 3) OR
    (nivel_ensino = 'SUPERIOR' AND ano IS NULL)
);

CREATE OR REPLACE VIEW view_active_requests AS
SELECT s.*
FROM solicitacao s
WHERE s.status IN ('PENDENTE', 'APROVADA')
  AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP);

CREATE OR REPLACE FUNCTION update_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_usuario_atualizado_em
BEFORE UPDATE ON usuario
FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em();

CREATE TRIGGER trigger_material_atualizado_em
BEFORE UPDATE ON material
FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em();

CREATE TRIGGER trigger_solicitacao_atualizado_em
BEFORE UPDATE ON solicitacao
FOR EACH ROW
EXECUTE FUNCTION update_atualizado_em();

-- ============================================================================
-- NOTES
-- ============================================================================

-- `password_hash` is the only persisted credential field for authentication.
-- `google_id`, `deleted_at`, audit-log tables, and LGPD anonymization tables
-- are not part of the current runtime schema.
-- FCM inbox/retry persistence and non-receipt reporting tables are already
-- part of the delivered runtime schema.
