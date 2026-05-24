CREATE DOMAIN IF NOT EXISTS role_enum AS VARCHAR(32)
    CHECK (VALUE IN ('USER', 'ADMIN'));

CREATE DOMAIN IF NOT EXISTS necessidade_academica_enum AS VARCHAR(64)
    CHECK (VALUE IN (
        'TEXTBOOKS',
        'WORKBOOKS',
        'REFERENCE_MATERIALS',
        'FICTION',
        'TECHNICAL_BOOKS',
        'TEST_PREP'
    ));

CREATE DOMAIN IF NOT EXISTS disciplina_enum AS VARCHAR(32)
    CHECK (VALUE IN (
        'TODAS',
        'MATEMATICA',
        'PORTUGUES',
        'HISTORIA',
        'GEOGRAFIA',
        'CIENCIAS',
        'LITERATURA'
    ));

CREATE DOMAIN IF NOT EXISTS nivel_ensino_enum AS VARCHAR(32)
    CHECK (VALUE IN ('FUNDAMENTAL', 'MEDIO', 'SUPERIOR'));

CREATE DOMAIN IF NOT EXISTS sistema_ensino_enum AS VARCHAR(32)
    CHECK (VALUE IN (
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
    ));

CREATE DOMAIN IF NOT EXISTS estado_conservacao_enum AS VARCHAR(32)
    CHECK (VALUE IN ('NOVO', 'BOM', 'USADO', 'DANIFICADO'));

CREATE DOMAIN IF NOT EXISTS status_material_enum AS VARCHAR(32)
    CHECK (VALUE IN ('DISPONIVEL', 'RESERVADO', 'DOADO', 'CANCELADO'));

CREATE DOMAIN IF NOT EXISTS status_ia_enum AS VARCHAR(32)
    CHECK (VALUE IN ('SUCCESS', 'LOW_CONFIDENCE', 'FAILURE', 'NOT_ATTEMPTED'));

CREATE DOMAIN IF NOT EXISTS status_solicitacao_enum AS VARCHAR(32)
    CHECK (VALUE IN ('PENDENTE', 'APROVADA', 'RECUSADA', 'CANCELADA', 'CONCLUIDA'));

ALTER TABLE IF EXISTS usuario
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- Keep the local H2 bootstrap safe for a first boot on an empty database.
-- This INIT script runs before Hibernate creates tables, so data backfills that
-- assume `usuario` already exists must not execute here.
-- Legacy pre-password local H2 files should be recreated instead of relying on
-- this early bootstrap path to mutate persisted rows.

ALTER TABLE IF EXISTS usuario
    ALTER COLUMN whatsapp DROP NOT NULL;

ALTER TABLE IF EXISTS usuario
    ALTER COLUMN cidade DROP NOT NULL;

ALTER TABLE IF EXISTS usuario
    ALTER COLUMN bairro DROP NOT NULL;

DROP INDEX IF EXISTS idx_usuario_google_id;

ALTER TABLE IF EXISTS usuario
    DROP COLUMN IF EXISTS google_id;
