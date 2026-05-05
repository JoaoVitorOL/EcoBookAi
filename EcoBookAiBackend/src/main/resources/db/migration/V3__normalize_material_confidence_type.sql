ALTER TABLE material
    ALTER COLUMN confianca_ia TYPE NUMERIC(3, 2)
    USING ROUND(confianca_ia::numeric, 2);
