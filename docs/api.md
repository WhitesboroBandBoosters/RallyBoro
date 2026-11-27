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

Future endpoint slices must update this document alongside tests. Specify request/response fields, validation, permission scope, HTTP errors, and duplicate/retry behavior. Use 400 for malformed input, 401 for missing authentication, 403 for insufficient permission, and 409 for business-state conflicts where appropriate. Do not expose privileged persistence APIs to the browser. API clients, agents, and UI must use the same authorization and business rules. Authentication/CSRF and a machine-readable business API contract accompany initiative 2 rather than being guessed for nonexistent operations.
