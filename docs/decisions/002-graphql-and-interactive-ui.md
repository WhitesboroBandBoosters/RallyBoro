---
title: GraphQL and interactive frontend direction
---

# GraphQL and interactive frontend direction

User direction · November 27, 2026 · Supersedes the business API and long-term UI assumptions in [foundation decision 001](001-foundation.md).

## Required direction

The application API for browser users, agents, and integrations is **GraphQL**. Build the staffing product around an interactive responsive frontend, with equally important mobile and desktop workflows. The existing Thymeleaf landing page is acceptable as the temporary initiative-1 shell; it does not select server-rendered HTML/form posts as the product interaction model.

Initiative 2 implements this direction with Spring GraphQL and an interactive React/TypeScript staffing client. The existing HTTP status and Actuator routes remain diagnostics. See [decision 003](003-staffing-pilot.md) for the selected tools and pilot rules.

## Backend and contract

Use Spring for GraphQL within the existing Spring Boot application, with a checked-in schema-first SDL contract. `/graphql` is the proposed HTTP endpoint. Queries and mutations express application use cases through shared services; JDBC, Flyway, and PostgreSQL remain in place. Keep operational HTTP health/status routes separate from business operations. Do not create parallel REST staffing endpoints or form-only write paths.

Define nullability, input validation, stable identifiers, bounded pagination, domain error codes, and mutation retry semantics as each slice is delivered. Capacity and overlap checks still run atomically in database transactions; GraphQL mutation execution order does not replace database concurrency controls. Define schema compatibility/deprecation practices rather than copying `/api/v1` URL versioning into the business schema.

Distinguish transport failures, GraphQL validation/execution errors, and expected domain failures. Clients must handle `errors` and possible partial `data`; HTTP 200 alone does not establish business success. Keep sensitive exception details out of errors and logs. The exact typed mutation-result/error convention is a phase-2 contract decision.

## Authentication, privacy, and agents

Spring Security OIDC remains the browser identity integration. Resolve session/CSRF handling and an appropriate scoped, revocable agent credential model in phase 2; GraphQL does not settle either. Agents must not depend on manually copying browser cookies. Apply authorization to organization-scoped operations and protected nested objects/fields, using shared business rules rather than permitting access merely because a top-level query passed. Query traversal, aliases, and batching must not bypass contact consent or participant privacy.

Set and test page-size, query depth/complexity, execution, and batch limits. Decide schema discovery and development tooling access while keeping the documented API usable by authorized agents. A first-party operation allowlist must not accidentally prohibit legitimate agent queries. No subscription transport, federation, or separate GraphQL gateway is required for the pilot.

## Frontend selection and behavior

Select an interactive framework and client in [issue #17](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/17). Evaluate React and Vue with strict TypeScript against maintainability and organizer/volunteer workflows. React is selected by decision 003; this note records the original candidate evaluation boundary. Generate operation-specific types from the checked-in schema and client operations, validate operations during builds, and retain server-side validation.

The product interface should support client-side navigation, inline validation, visible pending/loading/error/empty states, clear mutation results, and refreshed capacity/status after changes. Preserve keyboard accessibility, deep links, and narrow/wide layouts. Optimistic updates must never present a place as confirmed before authoritative acceptance. Refreshing data does not require subscriptions or offline replication.

Prefer same-origin application/API delivery and a simple container deployment. Select asset tooling, routing, and GraphQL client caching/invalidation in #17. Update the Docker build/context and CI when the frontend toolchain is introduced. Avoid building a second application platform merely to serve assets.

## Delivery and verification

[Issue #18](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/18) establishes the GraphQL transport, schema, conventions, limits, and a small non-sensitive query. Coordinate it with authentication issue #10. Add domain fields and interactive screens incrementally in #11–#14. Initiative 1 remains a valid operational milestone; GraphQL belongs before the first staffing slice, not after a complete form-based pilot.

Use Spring GraphQL test support and PostgreSQL Testcontainers for schema/resolver, authorization, and transactional behavior. Add schema/operation checks, client type checking, and browser interaction tests alongside each screen. Verify loading/failure states, nested-field privacy, cross-organization access, mutation retries, and concurrent last-slot outcomes through GraphQL as well as the UI.

References: [Spring GraphQL testing](https://docs.spring.io/spring-graphql/reference/testing.html) and [GraphQL demand controls](https://graphql.org/learn/security/).
