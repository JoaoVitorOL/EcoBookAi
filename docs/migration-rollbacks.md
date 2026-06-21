# Migration Rollback Inventory

Date: `2026-05-23`

## Goal

This document records the adopted rollback strategy for the Flyway chain under:

- `EcoBookAiBackend/src/main/resources/db/migration`

Rollback artifacts now live in:

- `EcoBookAiBackend/src/main/resources/db/rollback`

Snapshot execution is now documented directly in this file to keep the repository lean:

- use native `pg_dump` / `pg_restore` commands when a snapshot-only rollback is required

## Strategy

The repository now uses two rollback modes:

1. `SQL-backed rollback`
   - additive or structurally reversible migrations ship with a paired SQL rollback file
   - these are validated by applying the migration, executing rollback, deleting the matching Flyway history row and re-applying the migration in a fresh PostgreSQL schema

2. `Snapshot-required rollback`
   - historically destructive migrations ship with an explicit rollback artifact that aborts and instructs the operator to restore a pre-migration snapshot
   - this avoids pretending that dropped or normalized data can be losslessly reconstructed from the current schema alone

## Inventory

| Migration | Rollback Mode | Notes |
|----------|---------------|-------|
| `V1` | SQL-backed | Drops schema objects created by the initial bootstrap |
| `V2` | SQL-backed | Restores `NOT NULL` only when `usuario` data is already non-null |
| `V3` | SQL-backed | Reapplies the previous confidence type shape |
| `V4` | Snapshot-required | `google_id` was dropped and `password_hash` was synthesized |
| `V5` | SQL-backed | Removes Phase 3 upload-tracking columns/indexes |
| `V6` | SQL-backed | Removes `fcm_token` column/index |
| `V7` | SQL-backed | Removes author/publisher columns |
| `V8` | SQL-backed | Removes Phase 4 search indexes |
| `V9` | SQL-backed | Removes donation completion columns/index |
| `V10` | SQL-backed | Drops failed-notification queue artifacts |
| `V11` | SQL-backed | Recreates `disciplina_enum` without `TODAS`, guarded against persisted use |
| `V12` | Snapshot-required | `ano` values were normalized/clamped and the originals are not stored |
| `V13` | SQL-backed | Drops user notification inbox table/indexes |
| `V14` | SQL-backed | Drops non-receipt report table/type/indexes |
| `V15` | SQL-backed | Recreates `sistema_ensino_enum` without the extended values, guarded against persisted use |
| `V16` | SQL-backed | Renames report columns back, restores enum-backed status and old indexes |
| `V17` | SQL-backed | Removes Phase 8 LGPD/audit columns, indexes and tables |

## Automated Validation

The rollback process is now validated by:

- `EcoBookAiBackend/src/test/java/com/ecobook/migration/MigrationRollbackValidationTest.java`

What it verifies:

1. every migration file has a paired rollback artifact
2. every SQL-backed migration can be:
   - applied in a fresh PostgreSQL schema
   - rolled back
   - removed from Flyway history
   - re-applied successfully
3. the historically irreversible migrations (`V4`, `V12`) fail fast with an explicit snapshot requirement

Validation command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd EcoBookAiBackend
mvn -Dtest=MigrationRollbackValidationTest test
```

Or from the repo root:

```powershell
cd .\EcoBookAiBackend
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-26'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
mvn -Dtest=MigrationRollbackValidationTest test
```

## Snapshot Workflow

For any destructive migration path such as `V4` or `V12`:

1. create a database snapshot before migration
2. apply the migration
3. if rollback is required, restore the snapshot instead of attempting SQL reconstruction

Example commands:

```powershell
pg_dump --format=custom --file .\backups\pre-v12.dump --dbname postgresql://ecobook:dev_password_123@localhost:5432/ecobook
pg_restore --clean --if-exists --no-owner --dbname postgresql://ecobook:dev_password_123@localhost:5432/ecobook .\backups\pre-v12.dump
```

## Phase 10 Status

`T229` is closed. The project no longer claims that all historical migrations are intrinsically reversible; instead it now ships:

- rollback SQL for every reversible migration
- explicit snapshot-only rollback artifacts for the destructive ones
- an automated PostgreSQL validation test for apply -> rollback -> re-apply
