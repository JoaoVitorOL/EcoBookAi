DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM material
        WHERE disciplina::text = 'TODAS'
    ) THEN
        RAISE EXCEPTION 'Rollback of V11 requires removing persisted TODAS values from material.disciplina before recreating disciplina_enum.';
    END IF;

    ALTER TYPE disciplina_enum RENAME TO disciplina_enum_v11;

    CREATE TYPE disciplina_enum AS ENUM (
        'MATEMATICA',
        'PORTUGUES',
        'HISTORIA',
        'GEOGRAFIA',
        'CIENCIAS',
        'LITERATURA'
    );

    ALTER TABLE material
        ALTER COLUMN disciplina TYPE disciplina_enum
        USING disciplina::text::disciplina_enum;

    DROP TYPE disciplina_enum_v11;
END $$;
