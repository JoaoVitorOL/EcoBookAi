ALTER TABLE material
    ADD COLUMN IF NOT EXISTS imagem_verso_url VARCHAR(500);

ALTER TABLE material_upload_tracking
    ADD COLUMN IF NOT EXISTS secondary_file_path VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS secondary_mime_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS secondary_file_size BIGINT;

ALTER TABLE material
    DROP CONSTRAINT IF EXISTS check_ano_validity;

ALTER TABLE material
    DROP CONSTRAINT IF EXISTS check_ano_with_nivel;

-- Normalize legacy rows before enforcing the stricter rule introduced in Phase 5/6.
-- Older builds allowed years up to 12 for any non-superior material, so persisted data
-- may now violate the domain rule (FUNDAMENTAL 1..9, MEDIO 1..3, SUPERIOR null).
UPDATE material
SET ano = NULL
WHERE nivel_ensino = 'SUPERIOR'
  AND ano IS NOT NULL;

UPDATE material
SET ano = CASE
    WHEN ano IS NULL OR ano < 1 THEN 1
    WHEN ano > 3 THEN 3
    ELSE ano
END
WHERE nivel_ensino = 'MEDIO'
  AND (ano IS NULL OR ano < 1 OR ano > 3);

UPDATE material
SET ano = CASE
    WHEN ano IS NULL OR ano < 1 THEN 1
    WHEN ano > 9 THEN 9
    ELSE ano
END
WHERE nivel_ensino = 'FUNDAMENTAL'
  AND (ano IS NULL OR ano < 1 OR ano > 9);

ALTER TABLE material
    ADD CONSTRAINT check_ano_with_nivel
    CHECK (
        (nivel_ensino = 'FUNDAMENTAL' AND ano BETWEEN 1 AND 9) OR
        (nivel_ensino = 'MEDIO' AND ano BETWEEN 1 AND 3) OR
        (nivel_ensino = 'SUPERIOR' AND ano IS NULL)
    );
