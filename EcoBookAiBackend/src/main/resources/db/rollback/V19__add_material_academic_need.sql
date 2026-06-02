DROP INDEX IF EXISTS idx_material_necessidade_academica;

ALTER TABLE material
    DROP COLUMN IF EXISTS necessidade_academica;
