ALTER TABLE material
    ALTER COLUMN confianca_ia TYPE DECIMAL(3, 2)
    USING ROUND(confianca_ia::numeric, 2);
