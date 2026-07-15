# Backup format migrations

`BackupPayload.schemaVersion` uses the same integer as `AppDatabase` version in `AppDatabase.kt`.

When Room's database version increases because of a real schema change:

1. Bump `BACKUP_FORMAT_VERSION` in `BackupPayload.kt` to match the new Room version.
2. Add one function `migrateVNToVN+1` in `BackupMigrator.kt`.
3. Register it in `migrateOneStep`.
4. Transform only fields that changed at the JSON model level; unknown legacy fields may be dropped after migration.

Compatibility rules:

- **Older backup, newer app** (`backupVersion < current`): run the migration chain up to current, then import.
- **Same version**: import directly after normalization.
- **Newer backup, older app** (`backupVersion > current`): block import; user must update the app.

Minimum supported backup version: `BackupMigrator.MIN_SUPPORTED_VERSION` (currently 1).
