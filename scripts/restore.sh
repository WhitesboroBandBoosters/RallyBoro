#!/bin/sh
set -eu
: "${RALLYBORO_DATABASE_CONTAINER:?Set the existing PostgreSQL container name}"
: "${RALLYBORO_DATABASE_USERNAME:?Set the database role}"
: "${RALLYBORO_RESTORE_DATABASE:?Set a separately created empty restore database}"
: "${RALLYBORO_DATABASE_NAME:?Set the source application database name}"
[ "$#" -eq 1 ] && [ -f "$1" ] || { echo 'Usage: restore.sh /path/to/backup.dump' >&2; exit 2; }
[ "$RALLYBORO_RESTORE_DATABASE" != "$RALLYBORO_DATABASE_NAME" ] || { echo 'Restore into a separate empty database.' >&2; exit 2; }
count=$(docker exec "$RALLYBORO_DATABASE_CONTAINER" psql -U "$RALLYBORO_DATABASE_USERNAME" -d "$RALLYBORO_RESTORE_DATABASE" -Atc "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'")
[ "$count" = 0 ] || { echo 'Restore target must be empty.' >&2; exit 2; }
docker exec -i "$RALLYBORO_DATABASE_CONTAINER" pg_restore -U "$RALLYBORO_DATABASE_USERNAME" -d "$RALLYBORO_RESTORE_DATABASE" --exit-on-error --single-transaction --no-owner --no-privileges < "$1"
echo 'Restore completed. Validate data and application startup before switching databases.'
