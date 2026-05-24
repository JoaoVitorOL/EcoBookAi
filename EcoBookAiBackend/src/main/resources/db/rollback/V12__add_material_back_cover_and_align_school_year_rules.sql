DO $$
BEGIN
    RAISE EXCEPTION 'Rollback of V12 requires restoring a pre-migration snapshot because ano values were normalized and the original data is not stored.';
END $$;
