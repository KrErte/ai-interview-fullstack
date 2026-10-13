#!/usr/bin/env bash
# Nightly dump of the Futureproof Postgres database (compose project "futureproof"); keeps 14 days.
# Installed on the VPS as /usr/local/bin/futureproof-backup.sh, run by /etc/cron.d/futureproof-backup:
#   15 3 * * * root /usr/local/bin/futureproof-backup.sh >> /var/log/futureproof-backup.log 2>&1
# Restore:  gunzip -c FILE | docker exec -i futureproof-postgres-1 psql -U ai -d ai
# The dumps stay on the same server; copy them off the server for real disaster recovery.
set -euo pipefail

DIR=/var/backups/futureproof
KEEP_DAYS=14
CONTAINER=futureproof-postgres-1

mkdir -p "$DIR"
chmod 700 "$DIR"
file="$DIR/futureproof-$(date +%Y%m%d-%H%M%S).sql.gz"
# write to .tmp first so a failed dump never looks like a good backup
docker exec "$CONTAINER" pg_dump -U ai -d ai --no-owner | gzip -9 > "$file.tmp"
mv "$file.tmp" "$file"
find "$DIR" -name 'futureproof-*.sql.gz' -mtime +"$KEEP_DAYS" -delete
echo "$(date -Is) backup ok: $file ($(stat -c %s "$file") bytes)"
