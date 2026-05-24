DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM usuario
        WHERE whatsapp IS NULL
           OR cidade IS NULL
           OR bairro IS NULL
    ) THEN
        RAISE EXCEPTION 'Rollback of V2 requires usuario.whatsapp, cidade and bairro to be non-null before restoring NOT NULL constraints.';
    END IF;
END $$;

ALTER TABLE usuario
    ALTER COLUMN whatsapp SET NOT NULL,
    ALTER COLUMN cidade SET NOT NULL,
    ALTER COLUMN bairro SET NOT NULL;
