CREATE INDEX IF NOT EXISTS idx_material_status_disciplina_nivel
    ON material(status, disciplina, nivel_ensino);

CREATE INDEX IF NOT EXISTS idx_material_status_cidade_bairro
    ON material(status, cidade, bairro);

CREATE INDEX IF NOT EXISTS idx_material_status_data_publicacao
    ON material(status, data_publicacao DESC);
