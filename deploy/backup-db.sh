#!/usr/bin/env bash
# Nightly dump of one environment's database, kept for RETAIN_DAYS days.
#
# An app with no backup is one bad migration, one bad query or one full disk away from losing
# everything — and the first time anyone checks for a backup is the day they need it. This is
# the minimum: a compressed pg_dump per night, old ones pruned. It is NOT a disaster plan on its
# own — a copy on the same disk dies with the disk. Ship the files somewhere else too (a
# provider snapshot, rclone to object storage, anything off this machine).
#
#   deploy/backup-db.sh deploy/app.env            # from cron, as the app's own user:
#   15 3 * * *  /home/app/dev/app/deploy/backup-db.sh /home/app/dev/app/deploy/app.env
#
# Restore:  gunzip -c <file>.sql.gz | psql -h <host> -U <owner role> -d <database>
set -euo pipefail

env_file="${1:?usage: $0 <env file>}"
RETAIN_DAYS="${RETAIN_DAYS:-14}"
BACKUP_DIR="${BACKUP_DIR:-$HOME/backups}"

val() { grep "^$1=" "$env_file" | cut -d= -f2- || true; }

url=$(val DB_URL)                       # jdbc:postgresql://host:port/db
host=$(sed -E 's#jdbc:postgresql://([^:/]+).*#\1#' <<<"$url")
port=$(sed -nE 's#jdbc:postgresql://[^:/]+:([0-9]+)/.*#\1#p' <<<"$url"); port=${port:-5432}
db=$(sed -E 's#.*/([^/?]+).*#\1#' <<<"$url")

# The migrator owns every object, so it can dump all of them; the app role may not.
user=$(val FLYWAY_USER);   pass=$(val FLYWAY_PASSWORD)
[[ -z "$user" ]] && { user=$(val DB_USERNAME); pass=$(val DB_PASSWORD); }

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"                  # a dump holds every row the app has
file="$BACKUP_DIR/${db}-$(date +%Y%m%d-%H%M%S).sql.gz"

PGPASSWORD="$pass" pg_dump -h "$host" -p "$port" -U "$user" -d "$db" --no-owner | gzip > "$file.part"
mv "$file.part" "$file"                  # a half-written dump never looks like a good one

find "$BACKUP_DIR" -name "${db}-*.sql.gz" -mtime +"$RETAIN_DAYS" -delete
echo "backup: $file ($(du -h "$file" | cut -f1))"
