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
RALLYBORO_LOCAL_ACCOUNTS=true
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

## Local accounts, invitations, and optional identity providers

Local accounts are enabled by default (`RALLYBORO_LOCAL_ACCOUNTS=true`). No provider credentials, enrollment file, or organization bootstrap is required: use Create account, privately save the one-time recovery key, sign in, and create an organization or accept a shared invitation. The account initially has no organization memberships. Configure `RALLYBORO_LOCAL_ACCOUNTS=false` to disable local login, registration, and recovery if an operator deliberately uses only configured external identities.

Passwords are BCrypt hashes stored separately from stable user IDs; names/usernames do not determine new users' domain IDs. New passwords require at least 15 characters and at most 72 UTF-8 bytes. Account creation, password changes, and recovery/key replacement share a per-address limit of ten requests per minute (`rallyboro.account-attempts-per-minute`). The reverse proxy may group several users under one address; this is a single-instance limit, not a distributed quota. Browser login retains its separate request limit.

Save the recovery key in private storage. A username and valid recovery key can reset a password; the key is consumed and replaced atomically. Password change requires the current password. Both reset and change invalidate existing local-password sessions and revoke the user's agent tokens. A signed-in local user can replace a lost key under Account settings by proving their current password. There is no email-based reset or operator UI that reveals passwords/recovery keys. Losing both password and key has no self-service recovery in this slice.

Organization managers create single-use seven-day invitations, shared manually through their chosen channel. Invitation links can grant ordinary membership, all-event management, organization management, or management of one selected event. Anyone holding an unused link may accept it after signing in; share it with the intended person. The signed-out screen reveals no invitation details. Links are returned once, only hashes are stored, and managers can revoke unused invitations. Removing the issuing manager also invalidates their outstanding invitations. Do not copy these links into logs/issues/screenshots. Organization permission changes cannot remove the last manager. Member removal preserves registration history and the person's ability to cancel their own existing registrations, while revoking scoped agent tokens.

The organization page lists members, invitations, event grants, and access history in pages of 25. Event and member selection uses the current corresponding page. Member names needed for assignment are available to authorized event managers; private contact data is not collected or exposed. See [decision 004](decisions/004-local-accounts-and-scoped-access.md) for scope and limits.

### Compatibility with the existing pilot

The optional `RALLYBORO_ACCOUNTS_FILE` points to an external enrollment file, protected for container UID 10001. Compose defaults to a checked-in empty placeholder containing no accounts or credentials. If provided, the file imports legacy pilot accounts once, retaining their IDs, memberships, participant records, and staffing history. Imported identities are recorded so restart does not overwrite changed passwords/permissions or re-enroll removed members. Pilot local credentials still use `username.name`, `username.role` (`ORGANIZER` or `VOLUNTEER`), and `username.password-hash`; legacy organizers migrate to organization-management permission. Imports are compatibility setup, not the normal user/organization creation flow. Old `RALLYBORO_PILOT_LOGIN` is superseded by `RALLYBORO_LOCAL_ACCOUNTS`.

The current pilot's random credentials remain in `/home/jeremy/.config/rallyboro/pilot-login-credentials`, mode 0600. Retrieve them privately on this computer. Imported accounts have no initial recovery key; a signed-in user can establish one under Account settings. Never commit credentials or place real accounts in the empty placeholder. Keep the existing pilot events private/hidden and reuse them for intentional evaluation.

### Optional OIDC

Google OIDC remains wired for externally enrolled identities, but no provider is required for local accounts. Supply standard Spring client ID/secret/redirect configuration externally; in this deployment the Google callback is `https://whites.buru-pride.ts.net:8443/login/oauth2/code/google`. Enroll the exact `issuer|subject` using `username.oidc-identity` in an external pilot import file. Mappings live separately from `app_user`. An OIDC identity alone grants no membership, and names/email matches never link accounts automatically. Self-service linking of local accounts to additional providers is future work, not implemented by this compatibility import. Actual provider setup/verification remains [issue #19](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/19).

No phone/email fields are collected in this slice. Guest registration, dependents, phone-sharing consent, email delivery, public anonymous browsing, and proximity discovery remain follow-ups; see [phase 3](../roadmap/03-broader-participation.md).

## Frontend and browser verification

`./mvnw clean verify` includes generated GraphQL operation validation, strict TypeScript checking, production asset build, and PostgreSQL-backed integration tests. It requires internet access on first dependency download. For frontend-only work under Node 24, run `npm ci` and `npm run dev` in `frontend/`; Vite proxies API/auth routes to a local Spring server on 8080. Use the same-origin packaged application for OIDC callback verification.

To test the complete UI in isolation:

```bash
./mvnw -B -ntp test-compile
export PATH="$PWD/target/frontend-tools/node:$PATH"
(cd frontend && npx playwright install chromium)
scripts/test-ui.sh
```

The test host is compiled only under test sources, binds host loopback 18080, and starts a disposable PostgreSQL container. Random synthetic test credentials live in `target/`, which is ignored; the script terminates the host afterward. The browser suite verifies desktop and narrow/mobile layouts with separate organizer, volunteer, and uninvolved helper sessions, plus self-service account creation, organization creation, invitation acceptance/revocation, scoped permission changes, last-manager protection, password change, and offline recovery. The suite accepts only `http://127.0.0.1:18080`, and the script refuses an already-serving host on that port. Run it through `scripts/test-ui.sh`; do not aim mutating browser tests at a shared deployment. Test event titles are readable and disposable. Trace/video capture is disabled to avoid recording login or token payloads. Failed-test artifacts can still contain synthetic inputs: keep all test output ignored/private. CI installs Chromium and repeats this workflow before building the application image.

Automated suites use the isolated application/disposable-database approach. As clarified November 28, 2026, intentional functional/visual evaluation may use dedicated pilot accounts and a pilot organization in the shared deployment. Maintain a small reusable set of clearly named private/hidden pilot events; do not create a new permanent event for every run. Clean up temporary synthetic records after evaluation and preserve genuine participant history. Checks outside that pilot scope remain read-only. A pilot organization does not make PUBLIC event links private. Intentional pilot data is separate from automated test output and must not be created as a side effect of shared-deployment startup.

Backup/restore covers users, memberships, staffing, retry receipts, token hashes, and migration history. Do not copy sessions or deployment credentials into a backup archive. Restore with external authentication configuration appropriate to the destination; validate membership and permissions before sharing access.
