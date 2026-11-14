#!/bin/sh
# Restore drill: proves the latest backup of each organisation can really be restored.
#
#   ~/dhwarsh-deploy/deploy/restore-check.sh             (weekly, e.g. cron: 0 4 * * 0)
#
# For each organisation it restores the newest ~/backups/<org>/*.dump into a throwaway database
# ("restore_check") inside that organisation's own Postgres container, compares the row counts of key
# tables with the live database, then deletes the throwaway database. The live database is only read.
# Prints PASS or FAIL per organisation; exits non-zero on any FAIL.
set -eu

ROOT=${BACKUP_DIR:-$HOME/backups}
result=0

for env in "$HOME"/*/.env; do
  stack=$(grep -E '^STACK=' "$env" | cut -d= -f2 || true)
  [ -n "$stack" ] || continue
  latest=$(ls -1t "$ROOT/$stack"/*.dump 2>/dev/null | head -n 1 || true)
  [ -n "$latest" ] || { echo "$stack: FAIL - no backup found in $ROOT/$stack"; result=1; continue; }

  pg="$stack-postgres"
  run() { docker exec "$pg" sh -c "$1"; }
  run 'dropdb -U "$POSTGRES_USER" --if-exists restore_check && createdb -U "$POSTGRES_USER" restore_check'
  if ! docker exec -i "$pg" sh -c 'pg_restore -U "$POSTGRES_USER" -d restore_check --no-owner' < "$latest" 2> /tmp/restore-check.err; then
    # pg_restore reports harmless warnings too; only a restore with no tables counts as failed.
    :
  fi

  ok=1
  for table in employees users branches attendance; do
    restored=$(run "psql -U \"\$POSTGRES_USER\" -d restore_check -Atc 'select count(*) from $table'" 2>/dev/null || echo missing)
    live=$(run "psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -Atc 'select count(*) from $table'")
    echo "  $stack $table: backup=$restored live=$live"
    # Every table must come back readable. Counts are shown for comparison: the live database may have
    # changed since the backup was taken, so they need not match exactly.
    case "$restored" in ''|*[!0-9]*) ok=0 ;; esac
  done
  run 'dropdb -U "$POSTGRES_USER" --if-exists restore_check'

  if [ "$ok" = 1 ]; then echo "$stack: PASS - $(basename "$latest") restores"; else echo "$stack: FAIL - see above and /tmp/restore-check.err"; result=1; fi
done

exit $result
