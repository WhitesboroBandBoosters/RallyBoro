---
title: Running and recovering the foundation
---

# Running and recovering the foundation

## Requirements

Java 25, Docker Engine, and Compose. Maven installs Node 24.22.3 locally for the React build; no global Node installation is required for the Maven build. A Node 24 shell is needed for direct frontend or browser-test commands. `./mvnw -B -ntp clean verify` builds the executable JAR and runs integration tests in disposable PostgreSQL containers. Docker access is required; tests fail instead of silently skipping database coverage. No live database credentials are needed for tests. The browser-test host is test-only and also uses a disposable database.

PostgreSQL is operator-provided infrastructure. This repository does not provision it or store its credentials. Supply a dedicated database and non-superuser role with schema creation privileges for Flyway. Ensure the database is reachable on an existing Docker network.

## Application configuration

Keep your operator environment file outside the checkout, mode 0600, in a directory accessible only to the operator. It contains these non-secret connection settings and a password file path:

```text
RALLYBORO_DATABASE_URL=jdbc:postgresql://YOUR_DATABASE_HOST:5432/YOUR_APPLICATION_DATABASE
RALLYBORO_DATABASE_USERNAME=YOUR_APPLICATION_ROLE
RALLYBORO_DATABASE_NETWORK=YOUR_EXISTING_DOCKER_NETWORK
RALLYBORO_DATABASE_PASSWORD_FILE=/absolute/protected/path/application-password
RALLYBORO_ACCOUNTS_FILE=/absolute/protected/path/pilot-accounts.properties
RALLYBORO_PILOT_LOGIN=false
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

Use the HTTPS URL reported by Serve from a device authorized on the tailnet. This creates an additional tailnet-only listener; do not enable Funnel for the app. Keep unrelated Serve/Funnel listeners intact. Disable only this listener with `tailscale serve --https=8443 off`. Staffing is available to enrolled pilot users. Keep this deployment tailnet-only until broader-release access and real-data requirements are reviewed.

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

## Pilot enrollment and sign-in

The app mounts an external enrollment file as a read-only secret, accessible to container UID 10001. Protect it with the same directory permissions/read ACL as the database secret. Define each explicitly enrolled account with `username.name`, `username.role` (`ORGANIZER` or `VOLUNTEER`), and either `username.password-hash` for local pilot login or `username.oidc-identity` for OIDC. Local usernames are 1–40 letters, digits, underscores, or hyphens. `rallyboro.organization-id` and `rallyboro.organization-name` select the bootstrapped organization (default: RallyBoro Pilot). See [pilot rules](decisions/003-staffing-pilot.md) before changing enrollments or using real data.

For local synthetic testers, use unique random passwords encoded with BCrypt (for example a password-manager-generated password and an established BCrypt encoder). Only the hash goes into the enrollment file; keep the random cleartext credential in the operator's private credential store. Set `RALLYBORO_PILOT_LOGIN=true` explicitly. The deployed pilot's random credentials are stored in `/home/jeremy/.config/rallyboro/pilot-login-credentials`, mode 0600; retrieve them privately on this computer, not through chat or a shared terminal. No sample/default password is built into the app. This option is disabled by default.

For Google OIDC, supply Spring's `spring.security.oauth2.client.registration.google.client-id`, `client-secret`, and `redirect-uri` externally, with scopes `openid,profile`. In this deployment the callback is `https://whites.buru-pride.ts.net:8443/login/oauth2/code/google`. Configure the exact HTTPS redirect URI rather than trusting arbitrary forwarding headers. Supply those standard Spring properties via an operator-owned Compose override/configtree secret, never a tracked file. Enroll the exact `issuer|subject` as the account's `oidc-identity`; the application derives an opaque internal ID and does not persist email claims. Disable local pilot login if it is no longer needed. Provider credentials and a real Google round trip still require operator setup and verification.

No sign-up or invitation self-service is enabled yet. Merely authenticating does not authorize organization access. Display names and staffing records are limited to synthetic adult testers during this pilot. Do not collect phone/email details or use minors' records here.

## Frontend and browser verification

`./mvnw clean verify` includes generated GraphQL operation validation, strict TypeScript checking, production asset build, and PostgreSQL-backed integration tests. It requires internet access on first dependency download. For frontend-only work under Node 24, run `npm ci` and `npm run dev` in `frontend/`; Vite proxies API/auth routes to a local Spring server on 8080. Use the same-origin packaged application for OIDC callback verification.

To test the complete UI in isolation:

```bash
./mvnw -B -ntp test-compile
export PATH="$PWD/target/frontend-tools/node:$PATH"
(cd frontend && npx playwright install chromium)
scripts/test-ui.sh
```

The test host is compiled only under test sources, binds host loopback 18080, and starts a disposable PostgreSQL container. Random synthetic test credentials live in `target/`, which is ignored; the script terminates the host afterward. The browser suite verifies desktop and narrow/mobile layouts with separate organizer and volunteer sessions. Trace/video capture is disabled to avoid recording login or token payloads. Failed-test artifacts can still contain synthetic inputs: keep all test output ignored/private. CI installs Chromium and repeats this workflow before building the application image.

Backup/restore covers users, memberships, staffing, retry receipts, token hashes, and migration history. Do not copy sessions or deployment credentials into a backup archive. Restore with external authentication configuration appropriate to the destination; validate membership and permissions before sharing access.
