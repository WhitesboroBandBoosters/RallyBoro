---
title: "Phase 1 \u2014 Runnable foundation"
---

# Phase 1 — Runnable foundation

## Outcome

Start RallyBoro from a fresh checkout and reach a minimal responsive page and health endpoint on the maintainer's infrastructure through Tailscale. Persist a small synthetic record across application restarts. This is the first runnable milestone.

## Scope and sequence

1. Use PostgreSQL for the first implementation. The database is operator-provided infrastructure, outside this repository. Select the migration tool, frontend approach, and application runtime arrangement.
2. Add the Spring Boot entry point, HTTP support, executable packaging, configuration profiles, health reporting, and a minimal UI shell. Keep Java/Spring Boot/Maven as established in the project.
3. Add durable storage and a versioned migration. Exercise persistence through a restricted development fixture rather than a publicly writable diagnostic endpoint.
4. Establish reproducible local startup and container packaging. Document ports, configuration, secret injection, persistent volumes, and the actual host arrangement. Configure Tailscale access and HTTPS appropriate to that arrangement without assuming public exposure.
5. Add CI build verification, sanitized logs, and a basic backup/restore procedure. Establish the API documentation mechanism and consistent validation/error conventions for subsequent slices.

## Decisions before implementation

The [foundation decision record](../docs/decisions/001-foundation.md) selects Thymeleaf, JDBC/Flyway, Spring Security OIDC for phase 2, and Docker/Tailscale access. Define how configuration and encryption keys will be supplied. Public pages, notification providers, and payment adapters do not need to be selected here.

## Completion evidence

- A fresh checkout builds with `./mvnw clean verify` and starts using documented commands.
- The container starts successfully; health and UI checks work from mobile and desktop over the intended access path.
- Restart preserves synthetic data, migrations apply reproducibly, and a backup can be restored.
- No production credentials are committed or included in logs/images. Development fixtures are unavailable in the deployed configuration.

## Boundary

No event-management feature is required yet. Keep this phase short and proceed to the first end-to-end staffing slice rather than expanding infrastructure. Depends on the existing Maven foundation; enables phase 2.

## Implementation issues

Track current progress in [initiative #1](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/1).

- [#3: docs: decide prototype persistence, frontend, identity, and hosting](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/3)
- [#4: feat: add runnable Spring Boot application and responsive shell](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/4)
- [#5: feat: add persistent storage and versioned migrations](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/5)
- [#6: build: package and deploy the prototype through Tailscale](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/6)
- [#7: ci: verify application builds and document recovery](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/7)
- [#8: docs: verify runnable foundation and establish API conventions](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/8)

## Implementation evidence

See the [initiative 1 verification record](../docs/initiative-1-verification.md) and [running guide](../docs/running.md). Staffing and contact collection remain phase-2 work.
