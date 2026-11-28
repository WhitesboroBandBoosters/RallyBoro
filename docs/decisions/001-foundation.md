---
title: Foundation architecture decisions
---

# Foundation architecture decisions

Accepted for initiative 1 · November 27, 2026

**Superseded in part:** [Decision 002](002-graphql-and-interactive-ui.md) requires GraphQL for business operations and an interactive typed frontend. Thymeleaf and the JSON status route describe only the existing foundation.

## Application and interface

Use Java 25 and Spring Boot 4.2.0 as already established. Use Spring MVC and Thymeleaf for the first responsive interface, backed by an application-owned JSON API. Compared with a React/TypeScript SPA, this keeps the first runnable slice in familiar Java, HTML, and CSS without a second build toolchain. UI controllers may render pages, but future business operations must live in shared application services used by API and UI. An interactive client is now required for initiative 2; React/TypeScript is selected in decision 003.

Use JDBC and explicit SQL rather than introducing an ORM for the tiny foundation schema. PostgreSQL is the selected database. Use Flyway SQL migrations instead of ad hoc startup DDL; compared with Liquibase, SQL migrations add less machinery for this PostgreSQL-first prototype. Never edit an applied migration; add a new version. Flyway clean is disabled.

## Runtime and secrets

Package one executable JAR and one non-root Docker application container. Compose joins an operator-provided external database network and binds the application only to host loopback port 8088. Tailscale Serve supplies HTTPS on a dedicated tailnet-only port, 8443, without changing the existing port-443 service. No Kubernetes or managed-cloud deployment is needed.

The database is independently managed infrastructure. Its container deployment, credentials, and secret files are not project source. Supply a JDBC URL, role, network, and an external password file. Compose mounts only the application password as a read-only secret; Spring configuration-tree loading avoids a password in Compose environment values. The image context uses an allowlist to exclude local files, infrastructure, and credentials. Use a non-superuser database role restricted to the application database. It owns schema migrations for this prototype; split migration/runtime privileges when the staffing data model is introduced if operational needs warrant it.

Do not collect volunteer contact information in initiative 1. Encryption/key recovery and collection rules must be resolved before real personal data in initiative 2. A database login password is not a contact-encryption key. Backups remain protected external files.

## Identity for initiative 2

Select Spring Security OAuth2/OIDC client support, using an established provider such as Google, with application sessions and explicit organization membership authorization. This aligns with the maintainer's Java/Spring familiarity and earlier identity direction without custom OAuth. Provider registration and the final pilot access/session rules remain phase-2 work. No identity provider credentials are needed for the read-only foundation. Tailscale membership is never a substitute for application authorization.

## API and diagnostics

The existing diagnostic status route is under `/api/v1`; this is not the business API versioning strategy. Decision 002 selects GraphQL for initiative-2 business operations. Publish the small contract in [API conventions](../api.md). Use normal HTTP status codes and RFC 9457 problem details for supported framework errors. No database exception text, stack trace, contact, or secret should be returned. The aggregate health endpoint includes database connectivity; detailed actuator endpoints are not exposed. Current endpoints are read-only and contain no user data. Future write endpoints require authorization, validation, explicit retry semantics, and CSRF protection for browser session clients.

## Verification

Use JUnit and Spring Boot integration tests against disposable PostgreSQL through Testcontainers, never the operator's live database. CI runs the same Maven verification and builds the application image. Synthetic persistence probes are opt-in under the development profile and excluded whenever the production profile is active. There is no writable fixture HTTP endpoint.
