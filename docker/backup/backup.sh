#!/bin/sh
set -eu

umask 077

BACKUP_ROOT="${BACKUP_ROOT:-/backup}"
POSTGRES_HOST="${POSTGRES_HOST:-postgres}"
POSTGRES_DB="${POSTGRES_DB:-jsh_erp}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

case "$RETENTION_DAYS" in
  ''|*[!0-9]*) echo "BACKUP_RETENTION_DAYS must be a non-negative integer" >&2; exit 2 ;;
esac

mkdir -p "$BACKUP_ROOT"
timestamp="$(date '+%Y%m%d_%H%M%S')"
target_dir="$BACKUP_ROOT/$timestamp"
temporary_dir="$(mktemp -d "$BACKUP_ROOT/.in-progress.XXXXXX")"

cleanup() {
  rm -rf "$temporary_dir"
}
trap cleanup EXIT INT TERM

echo "[$(date '+%F %T %Z')] Starting jshERP backup"

# PostgreSQL custom format is compressed and is restored with pg_restore.
pg_dump --host="$POSTGRES_HOST" --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" \
  --format=custom --no-owner --no-privileges --file="$temporary_dir/database.dump"

# Uploaded attachments and installed plugins are persistent user data too.
tar -C /source -czf "$temporary_dir/files.tar.gz" upload plugins

(
  cd "$temporary_dir"
  sha256sum database.dump files.tar.gz > SHA256SUMS
  printf 'created_at=%s\npostgres_database=%s\n' "$(date -Iseconds)" "$POSTGRES_DB" > MANIFEST.txt
)

mv "$temporary_dir" "$target_dir"
trap - EXIT INT TERM

# Only dated backup directories directly below /backup are eligible for cleanup.
find "$BACKUP_ROOT" -mindepth 1 -maxdepth 1 -type d -name '20??????_??????' \
  -mtime "+$RETENTION_DAYS" -exec rm -rf {} +

echo "[$(date '+%F %T %Z')] Backup complete: $target_dir"
