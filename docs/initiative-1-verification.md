---
title: Initiative 1 verification
---

# Initiative 1 verification

November 27, 2026 · Runnable foundation

## Implemented

Java/Spring Boot executable application; responsive Thymeleaf shell; read-only versioned status API; aggregate/database-aware health; PostgreSQL JDBC; Flyway migrations; opt-in development fixture disabled under production; non-root app container and external configuration; CI build/image verification; application backup and transactional restore tooling.

The [decision record](decisions/001-foundation.md), [API conventions](api.md), and [running guide](running.md) describe the foundation and its boundaries. The PostgreSQL deployment and all real credential files remain local infrastructure, outside project commits.

## Local evidence

- `./mvnw -B -ntp clean verify`: six integration tests passed using disposable PostgreSQL 18.7 containers, with zero failures/skips. Tests cover the responsive shell, JSON contract, aggregate readiness, closed diagnostic endpoints, Flyway initialization, production fixture exclusion, and idempotent development fixture insertion.
- A multi-stage Docker build produced the executable app image, which started with the production profile as UID 10001 and a non-superuser PostgreSQL login. The application exposes only host loopback port 8088, joining an existing operator-provided database network.
- Tailscale Serve at `https://whites.buru-pride.ts.net:8443/` returns the shell, API status, and healthy database-aware readiness. Port 8443 is tailnet-only. The existing port-443 Funnel listener is unchanged.
- A synthetic `foundation_probe` row survived application restart; Flyway retained exactly one successful migration without reapplying it.
- The backup script created a protected custom-format dump outside the repository. The restore script loaded it into a separate empty database. The synthetic row and migration history matched, and a separate temporary application instance started against that restored database with readiness UP. The temporary application instance was removed afterward.
- Browser checks at 390×844 and 1440×1000 use the actual tailnet HTTPS URL, asserting the heading is visible, no horizontal overflow exists, and no JavaScript errors occur. These are emulated viewports on this host, not tests on physical phones or another tailnet machine.
- Secret-file values are outside the image context and Git. Runtime diagnostic exposure and a staged-content check guard against accidental disclosure. No real volunteer/contact records are present.

## Release boundary

This completes the infrastructure milestone, not the staffing pilot. Initiative 2 must define pilot authorization, participant identity, allowed event changes, minimum contact collection/verification, retention, and encryption/recovery before real personal data. The chosen identity integration is Spring Security OAuth2/OIDC, but no login or organization authorization is implemented in this read-only foundation.

Physical-device checks and access from another tailnet device remain useful operator follow-ups. No cloud hosting, public Funnel exposure for the app, payment, notification, or volunteer signup functionality is part of this release. CI outcome is recorded on the implementation pull request.
