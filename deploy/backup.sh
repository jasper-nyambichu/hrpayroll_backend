#!/bin/sh
# Nightly backup of every organisation on this server: its database and its uploaded documents.
#
#   ~/dhwarsh-deploy/deploy/backup.sh            (the deploy copies it there)
#   crontab: 30 2 * * * $HOME/dhwarsh-deploy/deploy/backup.sh >> $HOME/backups/backup.log 2>&1
#
# Writes ~/backups/<org>/<org>-<date>.dump (pg_dump custom format, restorable with pg_restore) and
# <org>-<date>-uploads.tar.gz, keeps KEEP_DAYS days, and checks each dump can be read back. Optional:
# OFFSITE_DIR=/mnt/somewhere copies each new file there too. A copy on the same disk does not protect
# against losing the server, so also copy ~/backups elsewhere regularly (see docs/RUNBOOK.md).
set -eu

KEEP_DAYS=${KEEP_DAYS:-14}
ROOT=${BACKUP_DIR:-$HOME/backups}
STAMP=$(date +%Y-%m-%d-%H%M)
failed=0

for env in "$HOME"/*/.env; do
  dir=$(dirname "$env")
  stack=$(grep -E '^STACK=' "$env" | cut -d= -f2 || true)
  [ -n "$stack" ] || continue                       # not an organisation folder
  docker inspect "$stack-postgres" >/dev/null 2>&1 || { echo "$(date) $stack: no $stack-postgres container, skipped"; continue; }

  mkdir -p "$ROOT/$stack"
  chmod 700 "$ROOT" "$ROOT/$stack"
  dump="$ROOT/$stack/$stack-$STAMP.dump"

  if docker exec "$stack-postgres" sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > "$dump" \
     && [ -s "$dump" ] \
     && docker exec -i "$stack-postgres" pg_restore -l < "$dump" > /dev/null; then
    chmod 600 "$dump"
    echo "$(date) $stack: database OK $(du -h "$dump" | cut -f1) $dump"
  else
    echo "$(date) $stack: DATABASE BACKUP FAILED"; rm -f "$dump"; failed=1; continue
  fi

  uploads="$ROOT/$stack/$stack-$STAMP-uploads.tar.gz"
  if docker volume inspect "${stack}_uploads" >/dev/null 2>&1; then
    docker run --rm -v "${stack}_uploads:/u:ro" alpine:3.20 tar czf - -C /u . > "$uploads" && chmod 600 "$uploads" \
      && echo "$(date) $stack: uploads OK $(du -h "$uploads" | cut -f1)" \
      || { echo "$(date) $stack: uploads backup failed"; rm -f "$uploads"; failed=1; }
  fi

  if [ -n "${OFFSITE_DIR:-}" ]; then
    mkdir -p "$OFFSITE_DIR/$stack" && cp "$dump" "$OFFSITE_DIR/$stack/" && { [ ! -f "$uploads" ] || cp "$uploads" "$OFFSITE_DIR/$stack/"; } \
      || { echo "$(date) $stack: offsite copy failed"; failed=1; }
  fi

  find "$ROOT/$stack" -type f -mtime +"$KEEP_DAYS" -delete
done

exit $failed
