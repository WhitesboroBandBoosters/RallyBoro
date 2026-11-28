---
title: Application API conventions
---

# Application API

The staffing API is schema-first GraphQL at **POST `/graphql`**, shared by browser clients and agents. The authoritative SDL is [schema.graphqls](https://github.com/WhitesboroBandBoosters/RallyBoro/blob/main/src/main/resources/graphql/schema.graphqls); named typed frontend operations are in [operations.graphql](https://github.com/WhitesboroBandBoosters/RallyBoro/blob/main/frontend/src/operations.graphql). Maven regenerates operation types and validates frontend operations on every build.

## Authentication

Browser clients sign in through configured Spring Security OIDC or the explicitly enabled controlled-pilot password login. Fetch `GET /auth/csrf` with same-origin credentials, then send its returned header name/token on login, logout, and GraphQL POST requests. Refetch the token after login/logout; sessions rotate. Never copy browser session cookies into agents. `GET /auth/options` reports enabled login options without credentials.

An authenticated browser user can create a scoped seven-day agent token under Agent access. Supply it as `Authorization: Bearer <token>` in an HTTPS GraphQL request; no cookie/CSRF token is required for a valid bearer credential. Tokens have organization and read/write scope, can be revoked, and cannot exceed the owner's permissions. Store the token privately and keep it out of command-line history, logs, and shared files.

## Operations

| Area | Operations |
| --- | --- |
| Discovery | `appInfo` (non-sensitive), `viewer` (own identity and memberships) |
| Events | `events`, `event`, `saveEvent` |
| Staffing structure | `saveJob`, `saveShift` |
| Participation | `signup`, `myRegistrations`, `cancelRegistration` |
| Organizer management | `members`, `coverage`, `assignVolunteer`, `removeAssignment` |
| Agent credentials | `agentTokens`, `createAgentToken`, `revokeAgentToken` (browser session only) |

For example, an authenticated client can request its authorized organization identifiers:

```graphql
query MyWorkspace {
  viewer { id displayName memberships { organizationId organizationName role } }
}
```

Events and list operations use `first` and `offset` (maximum first 100, offset 10000). Event lists return a total; other lists use page-length navigation. The pilot UI pages event lists, registrations, and coverage, and offers the first 100 organization members for assignment. Membership invitation/scaling beyond that pilot boundary is future work. Event nesting is also bounded to 20 jobs and 100 shifts per job.

Write inputs have bounded strings and validated date/time, visibility, revision, and capacity fields. The UI uses event-zone local times; API shift inputs are ISO instants. Signup/assignment require a fresh UUID `requestId` for a new intent; retry the same ID after an uncertain response. Reusing it for another action fails. Cancellation/removal are idempotent. Metadata edits need the current event `revision`; a stale edit must reload before retrying. Creation is not automatically retried. Business checks are transactional, not guaranteed by GraphQL's field execution order.

## Errors and limits

Transport/authentication/CSRF failures can return HTTP 4xx. GraphQL validation, execution, and domain errors can accompany HTTP 200 and partial/null `data`. Check both HTTP status and `errors`; success requires the intended operation result. Safe domain codes currently include `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `INVALID_INPUT`, `CONFLICT`, `CLOSED`, `FULL`, `OVERLAP`, and `LIMIT_EXCEEDED`. Internal exceptions do not expose SQL, stack traces, contacts, or credentials. GraphQL's transport and validation errors may have their own classifications; not every failure is a domain error.

Requests must be JSON with Content-Length, at most 64 KiB, with documents at most 32,000 characters. HTTP batching is disabled, maximum depth is 12, complexity 1000, and responses time out after 10 seconds. Lists remain bounded. Per-address pilot request limits return 429 with Retry-After. Introspection is available within the limits; authorization remains at the application boundary. Keep schema changes additive where possible, use deprecation before removal, and validate checked-in client operations in CI.

See [pilot decisions](decisions/003-staffing-pilot.md) for event visibility, edit policies, consent boundaries, and agent permissions.

## HTTP diagnostics and UI

`GET /` and event/registration deep links serve the interactive React client. `GET /api/v1/status` returns non-sensitive application identification; it is not a REST staffing API. Aggregate `/actuator/health`, `/actuator/health/readiness`, and `/actuator/health/liveness` remain available without component details. Readiness includes database connectivity. Other Actuator endpoints are not exposed. No fixture HTTP endpoint exists.
