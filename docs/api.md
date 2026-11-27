---
title: Application API conventions
---

# Application API conventions

The foundation has no events, volunteers, accounts, or authenticated business actions yet.

| Endpoint | Response | Purpose |
| --- | --- | --- |
| `GET /` | HTML | Responsive application shell. |
| `GET /api/v1/status` | `{"name":"RallyBoro","stage":"foundation"}` | Public, non-sensitive application identification. |
| `GET /actuator/health` | Aggregate status; 200 healthy, 503 unhealthy | Includes PostgreSQL connectivity, without component details. |
| `GET /actuator/health/readiness` | Aggregate status | Database-aware deployment readiness. |
| `GET /actuator/health/liveness` | Aggregate process status | Process liveness, separate from database availability. |

Send `Accept: application/json` for the JSON API. The status endpoint has no parameters and no side effects, so retrying is safe. Unsupported methods return 405. Unknown endpoints return 404. Spring MVC problem details are enabled for supported framework exceptions; clients must check status and content type rather than assuming every error has an identical body. Diagnostic responses contain no secrets or database connection details.

## Required business API — planned, not yet implemented

User direction requires **GraphQL** for UI, agents, and integrations. Implement Spring for GraphQL with checked-in schema SDL; `/graphql` is the proposed transport endpoint. The routes above remain diagnostics and do not establish a REST staffing API. See [decision 002](decisions/002-graphql-and-interactive-ui.md) and [issue #18](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/18).

Each domain slice must document query/mutation inputs and result types, nullability, bounded pagination, validation, permissions, domain errors, and safe retry behavior. Handle transport errors separately from GraphQL `errors` and partial `data`; an HTTP 200 response is not by itself a successful signup. The preceding HTTP status/problem-detail conventions describe the existing diagnostic routes, not the GraphQL domain error contract.

Authorization applies to nested object/field resolution as well as operations. Browser sessions/CSRF and scoped agent authentication need explicit integration. Keep private contacts out of unauthorized query paths and errors. Add Spring GraphQL integration tests with PostgreSQL Testcontainers, schema/operation validation, and operation-derived frontend types. Business invariants remain in shared application services and database transactions.
