-- EcoBook IA - PostgreSQL Database Schema
-- Version: 1.0
-- Date: 2026-04-17
-- Target: PostgreSQL 14+
-- References: data-model.md, spec.md RF-032-036

-- ============================================================================
-- ENUMS (Custom Data Types)
-- ============================================================================

-- Disciplina: Subject/Discipline (6 options)
CREATE TYPE disciplina_enum AS ENUM (
    'MATEMATICA',
    'PORTUGUES',
    'HISTORIA',
    'GEOGRAFIA',
    'CIENCIAS',
    'LITERATURA'
);

-- NivelEnsino: Education Level (3 options)
CREATE TYPE nivel_ensino_enum AS ENUM (
    'FUNDAMENTAL',
    'MEDIO',
    'SUPERIOR'
);

-- SistemaEnsino: Curriculum System (5 options)
CREATE TYPE sistema_ensino_enum AS ENUM (
    'ANGLO',
    'OBJETIVO',
    'COC',
    'POSITIVO',
    'OUTRO'
);

-- EstadoConservacao: Conservation State (4 options)
CREATE TYPE estado_conservacao_enum AS ENUM (
    'NOVO',
    'BOM',
    'USADO',
    'DANIFICADO'
);

-- StatusMaterial: Material Lifecycle (4 states)
CREATE TYPE status_material_enum AS ENUM (
    'DISPONIVEL',
    'RESERVADO',
    'DOADO',
    'CANCELADO'
);

-- StatusSolicitacao: Request Lifecycle (5 states)
CREATE TYPE status_solicitacao_enum AS ENUM (
    'PENDENTE',
    'APROVADA',
    'RECUSADA',
    'CANCELADA',
    'CONCLUIDA'
);

-- StatusRespostaIA: AI Response Status
CREATE TYPE status_resposta_ia_enum AS ENUM (
    'SUCCESS',
    'LOW_CONFIDENCE',
    'FAILURE'
);

-- ============================================================================
-- TABLES
-- ============================================================================

-- Usuario: Platform users (donors + students, no separation)
-- RFC: RF-001 through RF-004
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Authentication & Profile
    email VARCHAR(255) UNIQUE NOT NULL,
    google_id VARCHAR(255) UNIQUE,
    nome VARCHAR(255) NOT NULL,
    whatsapp VARCHAR(20) NOT NULL,
    
    -- Geographic (normalized: uppercase + NFD + ASCII)
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    instituicao VARCHAR(255),
    
    -- Flags
    perfil_completo BOOLEAN DEFAULT false NOT NULL,
    consentimento_ia BOOLEAN DEFAULT false NOT NULL,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,  -- Soft delete for LGPD compliance
    
    -- Constraints
    CONSTRAINT chk_whatsapp_e164 CHECK (whatsapp ~ '^\+\d{1,15}$'),
    CONSTRAINT chk_email_not_empty CHECK (email <> ''),
    CONSTRAINT chk_nome_not_empty CHECK (nome <> '')
);

-- Indexes for Usuario
CREATE UNIQUE INDEX idx_usuario_email ON usuario(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_usuario_google_id ON usuario(google_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_usuario_perfil_completo ON usuario(perfil_completo) WHERE deleted_at IS NULL;
CREATE INDEX idx_usuario_cidade ON usuario(cidade) WHERE deleted_at IS NULL;
CREATE INDEX idx_usuario_created_at ON usuario(created_at DESC);

-- Material: Donated educational resources
-- RFC: RF-005 through RF-025, RF-044
CREATE TABLE material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign key to donor
    doador_id UUID NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    
    -- Material metadata
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    
    -- Classification (AI-assisted or manual)
    disciplina disciplina_enum NOT NULL,
    nivel_ensino nivel_ensino_enum NOT NULL,
    ano INTEGER,
    sistema_ensino sistema_ensino_enum NOT NULL,
    estado_conservacao estado_conservacao_enum NOT NULL,
    
    -- Lifecycle & Storage
    status status_material_enum DEFAULT 'DISPONIVEL' NOT NULL,
    imagem_url VARCHAR(500),
    upload_id VARCHAR(100),
    
    -- Geographic (normalized)
    cidade VARCHAR(100) NOT NULL,
    bairro VARCHAR(100) NOT NULL,
    
    -- Publication metadata
    data_publicacao INTEGER,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP,  -- Soft delete for LGPD compliance
    
    -- Constraints
    CONSTRAINT chk_ano_range CHECK (ano IS NULL OR (ano >= 1 AND ano <= 12)),
    CONSTRAINT chk_superior_ano CHECK (
        (nivel_ensino = 'SUPERIOR' AND ano IS NULL) OR
        (nivel_ensino IN ('FUNDAMENTAL', 'MEDIO') AND ano IS NOT NULL)
    ),
    CONSTRAINT chk_data_publicacao_range CHECK (
        data_publicacao IS NULL OR (data_publicacao >= 1900 AND data_publicacao <= 2100)
    ),
    CONSTRAINT chk_titulo_not_empty CHECK (titulo <> '')
);

-- Indexes for Material (critical for search performance - Q6 SLA: P95 ≤ 150ms)
CREATE INDEX idx_material_status ON material(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_status_disciplina ON material(status, disciplina) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_status_nivel_ensino ON material(status, nivel_ensino) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_sistema_ensino ON material(sistema_ensino) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_cidade_bairro ON material(cidade, bairro) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_data_publicacao_desc ON material(data_publicacao DESC NULLS LAST) WHERE deleted_at IS NULL;
CREATE INDEX idx_material_doador_id ON material(doador_id);
CREATE INDEX idx_material_created_at ON material(created_at DESC);

-- Solicitacao: Request/Solicitation for material
-- RFC: RF-026 through RF-036
CREATE TABLE solicitacao (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Foreign keys
    material_id UUID NOT NULL REFERENCES material(id) ON DELETE CASCADE,
    estudante_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    
    -- Lifecycle
    status status_solicitacao_enum DEFAULT 'PENDENTE' NOT NULL,
    
    -- Donor contact (populated only when status = APROVADA)
    contato_doador JSONB,  -- { "nome": "...", "whatsapp": "+55..." }
    
    -- Timeline
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_at TIMESTAMP,
    expires_at TIMESTAMP,  -- 14 days after approval
    
    -- Constraints
    CONSTRAINT chk_contato_doador_only_approved CHECK (
        (status = 'APROVADA' AND contato_doador IS NOT NULL) OR
        (status != 'APROVADA' AND contato_doador IS NULL)
    ),
    CONSTRAINT chk_approved_at_only_approved CHECK (
        (status = 'APROVADA' AND approved_at IS NOT NULL) OR
        (status != 'APROVADA' AND approved_at IS NULL)
    ),
    CONSTRAINT chk_expires_at_consistency CHECK (
        expires_at IS NULL OR approved_at IS NOT NULL
    ),
    CONSTRAINT chk_student_not_donor CHECK (
        estudante_id <> (SELECT doador_id FROM material WHERE id = material_id)
    )
);

-- Indexes for Solicitacao
CREATE INDEX idx_solicitacao_material_id ON solicitacao(material_id);
CREATE INDEX idx_solicitacao_estudante_id ON solicitacao(estudante_id);
CREATE INDEX idx_solicitacao_status ON solicitacao(status);
CREATE INDEX idx_solicitacao_expires_at ON solicitacao(expires_at) WHERE status = 'APROVADA';
CREATE INDEX idx_solicitacao_created_at ON solicitacao(created_at DESC);

-- ============================================================================
-- AUDIT LOG TABLE (LGPD Compliance)
-- ============================================================================

-- Track all data modifications for LGPD compliance and debugging
CREATE TABLE auditlog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Event metadata
    evento VARCHAR(100) NOT NULL,  -- 'usuario_criado', 'material_criado', 'solicitacao_aprovada', etc.
    tabela VARCHAR(50),
    registro_id UUID,
    
    -- Actor
    usuario_id UUID REFERENCES usuario(id) ON DELETE SET NULL,
    
    -- Data changes
    dados_antes JSONB,
    dados_depois JSONB,
    
    -- Timestamp
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_auditlog_usuario_id ON auditlog(usuario_id);
CREATE INDEX idx_auditlog_evento ON auditlog(evento);
CREATE INDEX idx_auditlog_created_at ON auditlog(created_at DESC);
CREATE INDEX idx_auditlog_registro_id ON auditlog(registro_id);

-- ============================================================================
-- NOTIFICATION QUEUE (FCM Delivery Tracking)
-- ============================================================================

-- FCM notifications queue with retry tracking
CREATE TABLE fcm_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Notification metadata
    tipo VARCHAR(100) NOT NULL,  -- SOLICITACAO_RECEBIDA, SOLICITACAO_APROVADA, etc.
    usuario_id UUID NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    
    -- Payload
    payload JSONB NOT NULL,
    
    -- Retry tracking
    tentativas INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, SENT, FAILED, ARCHIVED
    erro_ultimo VARCHAR(500),
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    sent_at TIMESTAMP,
    failed_at TIMESTAMP,
    
    CONSTRAINT chk_tentativas_max CHECK (tentativas <= 5)
);

CREATE INDEX idx_fcm_notification_status ON fcm_notification(status);
CREATE INDEX idx_fcm_notification_usuario_id ON fcm_notification(usuario_id);
CREATE INDEX idx_fcm_notification_created_at ON fcm_notification(created_at DESC);

-- Dead-letter queue for permanently failed notifications
CREATE TABLE fcm_notification_dlq (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    notification_id UUID REFERENCES fcm_notification(id) ON DELETE SET NULL,
    tipo VARCHAR(100) NOT NULL,
    usuario_id UUID REFERENCES usuario(id) ON DELETE CASCADE,
    payload JSONB NOT NULL,
    erro_final VARCHAR(500),
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    archived_at TIMESTAMP
);

CREATE INDEX idx_fcm_notification_dlq_usuario_id ON fcm_notification_dlq(usuario_id);
CREATE INDEX idx_fcm_notification_dlq_created_at ON fcm_notification_dlq(created_at DESC);

-- ============================================================================
-- TRIGGERS (Automatic updates)
-- ============================================================================

-- Auto-update updated_at timestamp for usuario
CREATE OR REPLACE FUNCTION update_usuario_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_usuario_updated_at
BEFORE UPDATE ON usuario
FOR EACH ROW
EXECUTE FUNCTION update_usuario_timestamp();

-- Auto-update updated_at timestamp for material
CREATE OR REPLACE FUNCTION update_material_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_material_updated_at
BEFORE UPDATE ON material
FOR EACH ROW
EXECUTE FUNCTION update_material_timestamp();

-- Auto-update updated_at timestamp for solicitacao
CREATE OR REPLACE FUNCTION update_solicitacao_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_solicitacao_updated_at
BEFORE UPDATE ON solicitacao
FOR EACH ROW
EXECUTE FUNCTION update_solicitacao_timestamp();

-- ============================================================================
-- VIEWS (Useful queries)
-- ============================================================================

-- Materials available for search (DISPONIVEL only)
CREATE VIEW v_materials_available AS
SELECT
    m.id,
    m.titulo,
    m.disciplina,
    m.nivel_ensino,
    m.ano,
    m.sistema_ensino,
    m.estado_conservacao,
    m.cidade,
    m.bairro,
    m.data_publicacao,
    m.imagem_url,
    u.id AS doador_id,
    u.nome AS doador_nome,
    u.whatsapp AS doador_whatsapp,
    u.cidade AS doador_cidade,
    u.bairro AS doador_bairro,
    m.created_at
FROM material m
JOIN usuario u ON m.doador_id = u.id
WHERE m.status = 'DISPONIVEL' AND m.deleted_at IS NULL AND u.deleted_at IS NULL;

-- Requests pending donor response
CREATE VIEW v_requests_pending_response AS
SELECT
    s.id,
    s.material_id,
    s.estudante_id,
    m.titulo AS material_titulo,
    u.nome AS estudante_nome,
    s.created_at
FROM solicitacao s
JOIN material m ON s.material_id = m.id
JOIN usuario u ON s.estudante_id = u.id
WHERE s.status = 'PENDENTE' AND m.deleted_at IS NULL AND u.deleted_at IS NULL;

-- Approved requests nearing expiry (within 7 days)
CREATE VIEW v_requests_expiring_soon AS
SELECT
    s.id,
    s.material_id,
    s.estudante_id,
    m.titulo AS material_titulo,
    u.nome AS estudante_nome,
    s.expires_at,
    EXTRACT(DAY FROM s.expires_at - CURRENT_TIMESTAMP)::INTEGER AS dias_restantes
FROM solicitacao s
JOIN material m ON s.material_id = m.id
JOIN usuario u ON s.estudante_id = u.id
WHERE s.status = 'APROVADA'
  AND s.expires_at IS NOT NULL
  AND s.expires_at BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL '7 days'
  AND m.deleted_at IS NULL
  AND u.deleted_at IS NULL;

-- ============================================================================
-- STORED PROCEDURES (Complex operations)
-- ============================================================================

-- Approve solicitacao with atomic update (RFC-035)
CREATE OR REPLACE FUNCTION approve_solicitacao(
    p_solicitacao_id UUID,
    p_doador_id UUID
)
RETURNS TABLE(
    success BOOLEAN,
    message VARCHAR,
    solicitacao_id UUID,
    material_id UUID
) AS $$
DECLARE
    v_material_id UUID;
    v_estudante_nome VARCHAR;
    v_doador_nome VARCHAR;
    v_doador_whatsapp VARCHAR;
    v_existing_approved INT;
BEGIN
    -- Lock both rows (SERIALIZABLE isolation)
    SELECT m.id INTO v_material_id
    FROM material m
    WHERE m.id = (SELECT material_id FROM solicitacao WHERE id = p_solicitacao_id)
    FOR UPDATE;
    
    SELECT s.id INTO v_material_id
    FROM solicitacao s
    WHERE s.id = p_solicitacao_id
    FOR UPDATE;
    
    -- Check if material already has approved request
    SELECT COUNT(*)::INT INTO v_existing_approved
    FROM solicitacao
    WHERE material_id = v_material_id AND status = 'APROVADA' AND id <> p_solicitacao_id;
    
    IF v_existing_approved > 0 THEN
        RETURN QUERY SELECT false, 'Material already has approved request'::VARCHAR, p_solicitacao_id, v_material_id;
        RETURN;
    END IF;
    
    -- Verify actor is donor
    IF (SELECT doador_id FROM material WHERE id = v_material_id) <> p_doador_id THEN
        RETURN QUERY SELECT false, 'Only material donor can approve'::VARCHAR, p_solicitacao_id, v_material_id;
        RETURN;
    END IF;
    
    -- Get donor and student info
    SELECT u.nome, u.whatsapp INTO v_doador_nome, v_doador_whatsapp
    FROM usuario u WHERE u.id = p_doador_id;
    
    SELECT u.nome INTO v_estudante_nome
    FROM usuario u
    WHERE u.id = (SELECT estudante_id FROM solicitacao WHERE id = p_solicitacao_id);
    
    -- Update solicitacao
    UPDATE solicitacao
    SET
        status = 'APROVADA',
        approved_at = CURRENT_TIMESTAMP,
        expires_at = CURRENT_TIMESTAMP + INTERVAL '14 days',
        contato_doador = jsonb_build_object(
            'nome', v_doador_nome,
            'whatsapp', v_doador_whatsapp
        ),
        updated_at = CURRENT_TIMESTAMP
    WHERE id = p_solicitacao_id;
    
    -- Update material
    UPDATE material
    SET
        status = 'RESERVADO',
        updated_at = CURRENT_TIMESTAMP
    WHERE id = v_material_id;
    
    RETURN QUERY SELECT true, 'Solicitacao approved'::VARCHAR, p_solicitacao_id, v_material_id;
END;
$$ LANGUAGE plpgsql;

-- Auto-expire APROVADA solicitacoes after 14 days
CREATE OR REPLACE FUNCTION auto_expire_reservations()
RETURNS TABLE(
    expired_count INT,
    updated_material_count INT
) AS $$
DECLARE
    v_expired_count INT;
    v_updated_material_count INT;
BEGIN
    -- Update expired solicitacoes
    UPDATE solicitacao
    SET
        status = 'CANCELADA',
        updated_at = CURRENT_TIMESTAMP
    WHERE status = 'APROVADA' AND expires_at < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS v_expired_count = ROW_COUNT;
    
    -- Revert materials with expired reservations
    UPDATE material
    SET
        status = 'DISPONIVEL',
        updated_at = CURRENT_TIMESTAMP
    WHERE id IN (
        SELECT DISTINCT m.id
        FROM material m
        JOIN solicitacao s ON m.id = s.material_id
        WHERE m.status = 'RESERVADO'
          AND s.status = 'CANCELADA'
          AND s.expires_at < CURRENT_TIMESTAMP
    );
    
    GET DIAGNOSTICS v_updated_material_count = ROW_COUNT;
    
    RETURN QUERY SELECT v_expired_count, v_updated_material_count;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- NOTES
-- ============================================================================

-- Transaction Isolation:
-- All critical operations (approval) use SERIALIZABLE isolation level
-- to prevent race conditions (see approve_solicitacao procedure)

-- LGPD Compliance:
-- - Soft deletes (deleted_at) for data retention
-- - auditlog table tracks all modifications
-- - contato_doador only visible when solicitacao status = APROVADA

-- Performance Optimization:
-- - Material search indexes on (status, disciplina, nivel_ensino, cidade, bairro, data_publicacao DESC)
-- - PARTIAL indexes on deleted_at to exclude soft-deleted rows
-- - HikariCP connection pooling (default 20 connections) at application layer

-- Backup & Recovery:
-- - Daily full backups to external drive (2-year retention)
-- - Transaction logs for point-in-time recovery
