DO $$
BEGIN
    RAISE EXCEPTION 'Rollback of V4 requires restoring a pre-migration snapshot because google_id was dropped and password_hash values were synthesized.';
END $$;
