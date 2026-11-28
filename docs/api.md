---
title: Application API conventions
---

# Application API

The staffing API is schema-first GraphQL at **POST `/graphql`**, shared by browser clients and agents. The authoritative SDL is [schema.graphqls](https://github.com/WhitesboroBandBoosters/RallyBoro/blob/main/src/main/resources/graphql/schema.graphqls); named typed frontend operations are in [operations.graphql](https://github.com/WhitesboroBandBoosters/RallyBoro/blob/main/frontend/src/operations.graphql). Maven regenerates operation types and validates frontend operations on every build.

## Authentication

Browser clients sign in through Spring Security local username/password login or optional configured OIDC. Local accounts and organization creation require no external provider or enrollment file. Fetch `GET /auth/csrf` with same-origin credentials, then send its returned header name/token on login, logout, and GraphQL POST requests. Refetch the token after login/logout; sessions rotate. Never copy browser session cookies into agents. `GET /auth/options` reports enabled login options without credentials.

An authenticated browser user can create a scoped seven-day agent token under Account → Agent access. Supply it as `Authorization: Bearer <token>` in an HTTPS GraphQL request; no cookie/CSRF token is required for a valid bearer credential. Tokens have organization and read/write scope, can be revoked, and cannot exceed the owner's permissions. Store the token privately and keep it out of command-line history, logs, and shared files.

## Operations

| Area | Operations |
| --- | --- |
| Local accounts | `registerAccount`, `recoverAccount` (signed out), `changePassword`, `rotateRecoveryKey` (browser session) |
| Organizations | `createOrganization`, `setMembershipPermissions`, `removeMember` (browser session) |
| Invitations | `createInvitation`, `invitation`, `acceptInvitation`, `organizationInvitations`, `revokeInvitation` (browser session) |
| Scoped event access | `eventManagers`, `setEventManager`, `organizationAudit` (organization manager browser session) |
| Discovery | `appInfo` (non-sensitive), `viewer` (own identity and memberships) |
| Events | `events`, `event`, `saveEvent` |
| Staffing structure | `saveJob`, `saveShift` |
| Participation | `signupGuidance`, `signup`, `myRegistrations`, `cancelRegistration` |
| Organizer management | `members`, `coverage`, `assignVolunteer`, `removeAssignment` |
| Agent credentials | `agentTokens`, `createAgentToken`, `revokeAgentToken` (browser session only) |

Account creation returns a stable user ID and one-time recovery key; it does not create membership or sign the browser in. Password/recovery mutations use GraphQL variables over HTTPS with CSRF and must not be logged. Recovery/key replacement return a new one-time secret, never the stored hash. Password reset/change invalidate existing local-password sessions and revoke agent tokens. These operations are not an OAuth password grant.

`viewer.memberships` and `members` expose scoped `canManage` and `canOrganize` capabilities; legacy `role` is deprecated. Organization management implies all-event management; `Event.canManage` additionally reflects an individual event grant. A user can have different permissions in each context. Organization/member/invitation administration is browser-session-only in this slice. Scoped agent tokens continue to support authorized staffing operations and are bounded by their owner's current membership and event permissions; no permission comes from merely signing in. Account-free public participation is not yet enabled.

An invitation's `token` is returned once on creation, while listing returns metadata only. Signed-in preview shows the organization/scope before explicit acceptance. Tokens expire after seven days, are single-use, can be revoked, and depend on the issuer retaining management permission. Retrying acceptance as the same still-enrolled user is idempotent; another user or a removed member cannot reuse the link. List operations accept bounded `first`/`offset`. Access audit responses contain actions/actors/target IDs and no credential or invitation secret.

For example, an authenticated client can request its authorized organization identifiers:

```graphql
query MyWorkspace {
  viewer { id displayName memberships { organizationId organizationName role } }
}
```

Events and list operations use `first` and `offset` (maximum first 100, offset 10000). Event lists return a total; other lists use page-length navigation. The pilot UI pages event lists, registrations, and coverage, and offers the first 100 organization members for assignment. Membership invitation/scaling beyond that pilot boundary is future work. Event nesting is also bounded to 20 jobs and 100 shifts per job.

`signupGuidance(eventId: ID!)` returns each shift's ID, the caller's current confirmed registration ID (if any), and a time-conflict flag based on the caller's confirmed bookings across events. It applies the same visibility and token organization boundaries as `event`; it exposes no other participant's identity or registration details. The result is bounded by the event's staffing limits and does not depend on a page of registration history. Cancelled bookings do not cause conflicts, and adjacent shifts are allowed. Guidance is a snapshot: refresh to see changes made elsewhere, and always handle authoritative signup rejection if availability or commitments change.

Write inputs have bounded strings and validated date/time, visibility, revision, and capacity fields. The UI uses event-zone local times; API shift inputs are ISO instants. Signup/assignment require a fresh UUID `requestId` for a new intent; retry the same ID after an uncertain response. Reusing it for another action fails. Cancellation/removal are idempotent. Metadata edits need the current event `revision`; a stale edit must reload before retrying. Creation is not automatically retried. Business checks are transactional, not guaranteed by GraphQL's field execution order.

## Errors and limits

Transport/authentication/CSRF failures can return HTTP 4xx. GraphQL validation, execution, and domain errors can accompany HTTP 200 and partial/null `data`. Check both HTTP status and `errors`; success requires the intended operation result. Safe domain codes currently include `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `INVALID_INPUT`, `CONFLICT`, `CLOSED`, `FULL`, `OVERLAP`, and `LIMIT_EXCEEDED`. Internal exceptions do not expose SQL, stack traces, contacts, or credentials. GraphQL's transport and validation errors may have their own classifications; not every failure is a domain error.

Requests must be JSON with Content-Length, at most 64 KiB, with documents at most 32,000 characters. HTTP batching is disabled, maximum depth is 12, complexity 1000, and responses time out after 10 seconds. Lists remain bounded. Per-address pilot request limits return 429 with Retry-After. Introspection is available within the limits; authorization remains at the application boundary. Keep schema changes additive where possible, use deprecation before removal, and validate checked-in client operations in CI.

See [pilot decisions](decisions/003-staffing-pilot.md) for event visibility, edit policies, consent boundaries, and agent permissions.

## HTTP diagnostics and UI

`GET /` and event/registration deep links serve the interactive React client. `GET /api/v1/status` returns non-sensitive application identification; it is not a REST staffing API. Aggregate `/actuator/health`, `/actuator/health/readiness`, and `/actuator/health/liveness` remain available without component details. Readiness includes database connectivity. Other Actuator endpoints are not exposed. No fixture HTTP endpoint exists.
