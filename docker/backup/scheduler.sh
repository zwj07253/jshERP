#!/bin/sh
set -eu

BACKUP_TIME="${BACKUP_TIME:-02:30}"

# GNU date is available in the PostgreSQL Debian image. Validate the configured time once.
date -d "$(date +%F) $BACKUP_TIME" '+%s' >/dev/null

while true; do
  now="$(date '+%s')"
  scheduled="$(date -d "$(date +%F) $BACKUP_TIME" '+%s')"
  if [ "$scheduled" -le "$now" ]; then
    scheduled="$(date -d "tomorrow $BACKUP_TIME" '+%s')"
  fi
  wait_seconds=$((scheduled - now))
  echo "[$(date '+%F %T %Z')] Next backup is scheduled for $(date -d "@$scheduled" '+%F %T %Z')"
  sleep "$wait_seconds"

  if ! /usr/local/bin/backup.sh; then
    echo "[$(date '+%F %T %Z')] Backup failed; it will be retried at the next scheduled time" >&2
  fi
  # Prevent a same-second restart edge case from scheduling the just-finished run again.
  sleep 60
done
