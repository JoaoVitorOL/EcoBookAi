DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM material
        WHERE sistema_ensino::text IN ('POLIEDRO', 'ETAPA', 'BERNOULLI', 'SAS', 'FTD')
    ) THEN
        RAISE EXCEPTION 'Rollback of V15 requires migrating persisted extended sistema_ensino values before recreating sistema_ensino_enum.';
    END IF;

    ALTER TYPE sistema_ensino_enum RENAME TO sistema_ensino_enum_v15;

    CREATE TYPE sistema_ensino_enum AS ENUM (
        'ANGLO',
        'OBJETIVO',
        'COC',
        'POSITIVO',
        'OUTRO'
    );

    ALTER TABLE material
        ALTER COLUMN sistema_ensino TYPE sistema_ensino_enum
        USING sistema_ensino::text::sistema_ensino_enum;

    DROP TYPE sistema_ensino_enum_v15;
END $$;
