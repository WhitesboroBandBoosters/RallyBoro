---
title: Typed JPA persistence
---

# Typed JPA persistence

November 28, 2026 · Supersedes the foundation's handwritten JDBC persistence · Issue #22

## Decision

Use Spring Data JPA with Hibernate for application persistence. Do not put handwritten SQL, native queries, or textual JPQL/HQL in services or repositories. Use typed entities, derived Spring Data repository methods, and Criteria specifications referencing the generated JPA metamodel. Flyway SQL remains responsible for schema migrations; independent database test setup/assertions may use JDBC to verify stored results independently of ORM behavior. JDBC connection URLs and the PostgreSQL driver are still used underneath JPA.

The domain has ordinary relational relationships and needs no feature that prevents using a standard ORM. Capacity/overlap checks, idempotent signup, invitations, scoped access, password recovery, and last-manager protection require explicit transactions and locks, not raw SQL. Future spatial discovery may require typed spatial functions or an extension; it does not justify handwritten queries for the current product.

## Mapping and repository boundaries

Entity mappings cover the existing schema without changing IDs, constraints, migrations, GraphQL contracts, or stored records. Stable UUIDs and composite keys are mapped explicitly. Foreign-key IDs are the write representation; lazy read-only associations support typed traversal and derived joins. Fully initialize new entities before persistence. Application services consume typed entities/repositories and return DTOs; never expose managed entities through GraphQL or JSON.

Spring Data repositories handle CRUD, derived predicates, counts, pagination, and Specifications. Dynamic visibility and organization-scoped registration filtering use generated metamodel paths. Metamodel generation is part of the Maven compile step using the Boot-managed Hibernate processor. A small shared repository base provides explicit persist and pessimistic lock/refresh operations; it is not a separate query language. Offset pagination preserves arbitrary API offsets instead of rounding to page boundaries.

## Transactions and locking

Preserve the existing explicit event revision and row-lock semantics. Keep event/participant/shift locking for signup/cancellation, organization locking for invitation and permission changes, and credential locking for recovery consumption. The shared lock operation refreshes state after acquiring a pessimistic write lock, including an entity loaded before a competing transaction committed. This avoids using stale first-level-cache state after waiting. Revision increments remain explicit under the event lock, preserving the API's previous behavior; they are not silently replaced by different dirty-check/version semantics.

Keep `spring.jpa.open-in-view=false`. Build DTOs inside service transactions. Configure bounded lazy batch fetching, use explicit fetch graphs where needed, and examine query counts/fetch plans as data grows. Do not switch everything to eager loading. In particular, personal signup guidance loads only confirmed registrations overlapping the event's shift window. Database queries still enforce capacity/conflicts after the locks; client guidance is advisory.

Hibernate validates mappings with `spring.jpa.hibernate.ddl-auto=validate`; it must not create or update production schemas. Flyway owns DDL and its checksums. This refactor needs no new migration. PostgreSQL partial uniqueness and retry-receipt constraints remain authoritative.

## Verification

Retain the independent PostgreSQL integration tests for concurrent last-place/overlap requests, retries, cancellations, recovery and invitation races, last-manager changes, authorization, and migrated data. Add an architecture check for direct JDBC/text-query use in application Java sources. Run the existing desktop/mobile Playwright workflows against a disposable app/database, build the container, and validate the existing deployed schema/data using a protected restored copy before deployment. Shared-deployment smoke checks remain read-only outside the intentional pilot scope.
