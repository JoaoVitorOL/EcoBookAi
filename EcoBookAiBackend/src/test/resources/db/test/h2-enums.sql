CREATE TYPE IF NOT EXISTS disciplina_enum AS ENUM (
    'MATEMATICA',
    'PORTUGUES',
    'HISTORIA',
    'GEOGRAFIA',
    'CIENCIAS',
    'LITERATURA'
);

CREATE TYPE IF NOT EXISTS nivel_ensino_enum AS ENUM (
    'FUNDAMENTAL',
    'MEDIO',
    'SUPERIOR'
);

CREATE TYPE IF NOT EXISTS sistema_ensino_enum AS ENUM (
    'ANGLO',
    'OBJETIVO',
    'COC',
    'POSITIVO',
    'OUTRO'
);

CREATE TYPE IF NOT EXISTS estado_conservacao_enum AS ENUM (
    'NOVO',
    'BOM',
    'USADO',
    'DANIFICADO'
);

CREATE TYPE IF NOT EXISTS status_material_enum AS ENUM (
    'DISPONIVEL',
    'RESERVADO',
    'DOADO',
    'CANCELADO'
);

CREATE TYPE IF NOT EXISTS status_solicitacao_enum AS ENUM (
    'PENDENTE',
    'APROVADA',
    'RECUSADA',
    'CANCELADA',
    'CONCLUIDA'
);

CREATE TYPE IF NOT EXISTS status_ia_enum AS ENUM (
    'SUCCESS',
    'LOW_CONFIDENCE',
    'FAILURE',
    'NOT_ATTEMPTED'
);

CREATE TYPE IF NOT EXISTS role_enum AS ENUM (
    'USER',
    'ADMIN'
);

CREATE TYPE IF NOT EXISTS necessidade_academica_enum AS ENUM (
    'TEXTBOOKS',
    'WORKBOOKS',
    'REFERENCE_MATERIALS',
    'FICTION',
    'TECHNICAL_BOOKS',
    'TEST_PREP'
);
