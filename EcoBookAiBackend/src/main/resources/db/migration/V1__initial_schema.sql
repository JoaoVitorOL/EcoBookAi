-- EcoBook IA - Initial Database Schema
-- Version: 1.0
-- Created: 2026-04-17

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create enums
CREATE TYPE disciplina_enum AS ENUM (
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

-- Create tables
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    whatsapp VARCHAR(20) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    instituicao VARCHAR(255),
    perfil_completo BOOLEAN NOT NULL DEFAULT false,
    consentimento_ia BOOLEAN NOT NULL DEFAULT false,
    google_id VARCHAR(255) UNIQUE,
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
    descricao TEXT,
    disciplina disciplina_enum NOT NULL,
    nivel_ensino nivel_ensino_enum NOT NULL,
    ano INTEGER,
    sistema_ensino sistema_ensino_enum NOT NULL,
    estado_conservacao estado_conservacao_enum NOT NULL,
    status status_material_enum NOT NULL DEFAULT 'DISPONIVEL',
    imagem_url VARCHAR(500),
    upload_id VARCHAR(100),
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    data_publicacao INTEGER,
    status_ia status_ia_enum,
    confianca_ia DECIMAL(3, 2),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
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
    expires_at TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_google_id ON usuario(google_id);
CREATE INDEX idx_usuario_perfil_completo ON usuario(perfil_completo);

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

-- Create table for tracking AI processing for deduplication
CREATE TABLE material_upload_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id VARCHAR(100) NOT NULL UNIQUE,
    material_id UUID REFERENCES material(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_material_upload_tracking_upload_id ON material_upload_tracking(upload_id);
CREATE INDEX idx_material_upload_tracking_status ON material_upload_tracking(status);

-- Add constraints for data integrity
ALTER TABLE material
ADD CONSTRAINT check_ano_validity
CHECK (ano IS NULL OR (ano >= 1 AND ano <= 12));

ALTER TABLE material
ADD CONSTRAINT check_ano_with_nivel
CHECK (
    (nivel_ensino IN ('FUNDAMENTAL', 'MEDIO') AND ano IS NOT NULL) OR
    (nivel_ensino = 'SUPERIOR' AND ano IS NULL)
);

-- Create view for active requests
CREATE OR REPLACE VIEW view_active_requests AS
SELECT s.* FROM solicitacao s
WHERE s.status IN ('PENDENTE', 'APROVADA')
AND (s.expires_at IS NULL OR s.expires_at > CURRENT_TIMESTAMP);

-- Create function to update atualizado_em timestamp
CREATE OR REPLACE FUNCTION update_atualizado_em()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers to automatically update atualizado_em
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

-- Add comments for documentation
COMMENT ON TABLE usuario IS 'Platform users (both donors and recipients)';
COMMENT ON TABLE material IS 'Educational materials available for donation';
COMMENT ON TABLE solicitacao IS 'Requests for materials by students';
COMMENT ON TABLE material_upload_tracking IS 'Tracking of file uploads for deduplication';

COMMENT ON COLUMN material.confianca_ia IS 'Confidence score from AI classification (0.0 to 1.0)';
COMMENT ON COLUMN usuario.perfil_completo IS 'Flag indicating if user has completed mandatory profile fields';
COMMENT ON COLUMN usuario.consentimento_ia IS 'User consent for AI processing of uploaded images';
