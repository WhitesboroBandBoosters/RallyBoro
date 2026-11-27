#!/bin/sh
# Database tooling is supplied by the independently managed PostgreSQL container.
set -eu
: "${RALLYBORO_DATABASE_CONTAINER:?Set the existing PostgreSQL container name}"
: "${RALLYBORO_DATABASE_NAME:?Set the application database name}"
: "${RALLYBORO_DATABASE_USERNAME:?Set the database role}"
[ "$#" -eq 1 ] || { echo 'Usage: backup.sh /absolute/protected/backup.dump' >&2; exit 2; }
case "$1" in /*) ;; *) echo 'An absolute backup path is required.' >&2; exit 2;; esac
umask 077
partial=$(mktemp "${1}.partial.XXXXXX")
trap 'rm -f "$partial"' EXIT HUP INT TERM
docker exec "$RALLYBORO_DATABASE_CONTAINER" pg_dump -U "$RALLYBORO_DATABASE_USERNAME" -d "$RALLYBORO_DATABASE_NAME" --format=custom > "$partial"
# Atomic link refuses to overwrite and never publishes an incomplete dump.
ln "$partial" "$1"
echo 'Backup completed. Keep it outside the repository in protected storage.'
