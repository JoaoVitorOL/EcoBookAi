ALTER TABLE material
    ADD COLUMN IF NOT EXISTS necessidade_academica necessidade_academica_enum NOT NULL DEFAULT 'TEXTBOOKS';

CREATE INDEX IF NOT EXISTS idx_material_necessidade_academica
    ON material(necessidade_academica);
