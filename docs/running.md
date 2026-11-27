---
title: Running and recovering the foundation
---

# Running and recovering the foundation

## Requirements

Java 25, Docker Engine, and Compose. `./mvnw -B -ntp clean verify` builds the executable JAR and runs integration tests in disposable PostgreSQL containers. Docker access is required; tests fail instead of silently skipping database coverage. No live database credentials are needed for tests.

PostgreSQL is operator-provided infrastructure. This repository does not provision it or store its credentials. Supply a dedicated database and non-superuser role with schema creation privileges for Flyway. Ensure the database is reachable on an existing Docker network.

## Application configuration

Keep your operator environment file outside the checkout, mode 0600, in a directory accessible only to the operator. It contains these non-secret connection settings and a password file path:

```text
RALLYBORO_DATABASE_URL=jdbc:postgresql://YOUR_DATABASE_HOST:5432/YOUR_APPLICATION_DATABASE
RALLYBORO_DATABASE_USERNAME=YOUR_APPLICATION_ROLE
RALLYBORO_DATABASE_NETWORK=YOUR_EXISTING_DOCKER_NETWORK
RALLYBORO_DATABASE_PASSWORD_FILE=/absolute/protected/path/application-password
```

The password file contains only the application role password, never the PostgreSQL superuser credential. Restrict file access to the operator and container UID 10001, for example with a read ACL (`setfacl -m u:10001:r /absolute/protected/path/application-password`). Keep the containing directory mode 0700. Compose mounts the file read-only; the password is not placed in container environment metadata. Never commit the environment file or password file. Do not run `compose config` into shared output if adding secrets to environment values.

```bash
docker compose --env-file /absolute/path/application.env up -d --build
curl --fail http://127.0.0.1:8088/actuator/health/readiness
docker compose --env-file /absolute/path/application.env logs --tail=50 app
docker compose --env-file /absolute/path/application.env restart app
docker compose --env-file /absolute/path/application.env stop app
```

The app binds only `127.0.0.1:8088` on the host. The application container joins the existing database network. It runs with the `production` profile, UID 10001, dropped Linux capabilities, and no database administration socket. On boot Flyway validates checksums and applies new SQL migrations. Invalid credentials, incompatible migrations, or an unavailable database cause startup failure; correct the underlying configuration and restart. Do not bypass migration validation or delete history to force startup.

For local JAR development, export the URL, username, and `RALLYBORO_DATABASE_PASSWORD` privately into the process environment, using an operator-selected connection route. `java -jar target/rallyboro-0.0.1-SNAPSHOT.jar` starts the same application. Do not put a password on the command line. Docker-network-only database hosts are not resolvable from the host JVM; use the container workflow unless the operator supplies a separate local connection route.

## Tailnet access

```bash
tailscale serve --bg --https=8443 http://127.0.0.1:8088
tailscale serve status
```

Use the HTTPS URL reported by Serve from a device authorized on the tailnet. This creates an additional tailnet-only listener; do not enable Funnel for the app. Keep unrelated Serve/Funnel listeners intact. Disable only this listener with `tailscale serve --https=8443 off`. No personal data or staffing functions are available in this foundation.

## Persistence verification

The first migration creates `foundation_probe` and Flyway's history table. For a disposable development database only, enable the `development` profile and `rallyboro.fixture.enabled=true` at startup. The runner inserts one fixed synthetic record without an HTTP write route. Repeated startup preserves the record; production disables this runner even if the flag is set. Never use fixture mode with real volunteer data.

## Backup and restore

The scripts use the PostgreSQL client tools already supplied by operator infrastructure. Set `RALLYBORO_DATABASE_CONTAINER`, `RALLYBORO_DATABASE_NAME`, and `RALLYBORO_DATABASE_USERNAME` for the application database. They use a container-local database connection; the operator must provide authentication accepted by that infrastructure. They do not initialize containers or install credentials.

```bash
scripts/backup.sh /absolute/protected/path/foundation.dump
# Operator creates a separate empty database owned by the application role.
export RALLYBORO_RESTORE_DATABASE=YOUR_EMPTY_RECOVERY_DATABASE
scripts/restore.sh /absolute/protected/path/foundation.dump
```

Backups are custom-format dumps created mode 0600 without overwriting an existing file. Keep them outside the checkout and protect any future personal data. Restore refuses the source database name or a nonempty target, restores transactionally without source ownership/privileges, and stops on error. Verify records, Flyway history, and app health against the restored database before deliberately changing the application URL. A failed upgrade may require restoring both the compatible application image and a pre-upgrade database backup; an older JAR alone does not undo migrations. Preserve external password/encryption material separately.

## CI

`.github/workflows/application-ci.yml` runs Maven integration tests with Docker/Testcontainers and builds the image. A GitHub credential able to update workflows is needed when publishing changes to that file. The local equivalent is `./mvnw -B -ntp clean verify` followed by `docker build -t rallyboro:foundation .`.
