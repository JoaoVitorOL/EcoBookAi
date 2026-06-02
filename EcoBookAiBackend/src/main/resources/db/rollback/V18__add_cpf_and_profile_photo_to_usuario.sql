ALTER TABLE usuario
    DROP COLUMN IF EXISTS foto_perfil_mime_type,
    DROP COLUMN IF EXISTS foto_perfil_path,
    DROP COLUMN IF EXISTS cpf;
