---
title: Staffing pilot rules and selected tools
---

# Staffing pilot rules and selected tools

November 27, 2026 · Initiative 2 implementation

## Visual and content direction

Use a maroon-focused palette with neutral surfaces. Keep interface copy direct and functional: headings, labels, instructions, and status/error feedback. Avoid promotional taglines, inspirational slogans, and decorative introductory copy on login or application screens.

## Interactive client

Select React with strict TypeScript, React Router, Vite, and GraphQL Request. Vue was a viable alternative; React's component ecosystem and typed GraphQL integration fit the small interactive workflow. GraphQL Request is preferred over a normalized-cache client for this pilot because explicit post-mutation refetching is sufficient and avoids another state model. Queries and mutation inputs/results are generated from the checked-in schema and named operations; generated files and bundles are build outputs. Maven installs a pinned local Node runtime, runs locked dependency installation, validates operations, type-checks the client, and packages the same-origin assets. Docker builds the frontend in a separate build stage.

The first product includes an event list, event/job/shift editing, event signup, personal registrations/cancellation, organizer coverage/assignment/removal, and agent access management. Navigation, loading/empty/error states, narrow/wide layouts, and authoritative confirmation are part of each slice. There are no optimistic capacity reservations.

## Pilot participation and identity

This is a controlled, synthetic-data pilot with explicitly enrolled adult testers, not an unrestricted public launch. Browser identity uses Spring Security. Google OIDC is supported when the operator supplies provider configuration and pre-enrolls exact issuer/subject identities. Provider credentials and an actual Google round trip are not available in this checkout and must be supplied/verified by the operator.

To make the local pilot usable independently, an explicit `RALLYBORO_PILOT_LOGIN=true` enables Spring Security's password authentication for enrolled local accounts. It is off by default. Passwords are BCrypt hashes in an external operator-owned enrollment file; there are no source-controlled/default accounts or passwords. The deployment uses randomly generated synthetic test credentials outside the repository. This is a pilot login option, not custom OAuth or anonymous account creation. Authentication never implies membership. Sessions expire after 30 minutes idle, use HttpOnly/SameSite=Lax cookies, and use Secure cookies in the deployed Compose setup. Browser POSTs, including login/logout/GraphQL, require CSRF tokens.

An organization has ORGANIZER and VOLUNTEER memberships. Organizers manage that organization's events, staffing, and assignments; volunteers manage their own participation. There are no self-service invitations or role changes yet. Bootstrap enrollment upserts the configured users and memberships at startup. Removing a file entry alone does not delete historical records or existing OIDC memberships: operators must remove the membership and revoke sessions/tokens as appropriate. Business authorization reads membership on each action.

No phone, email, guardian, qualification evidence, or contact verification data is collected. OIDC email claims are not copied into application tables. Only opaque user IDs, pilot display names, memberships, and staffing records are persisted. Use synthetic identities; contact encryption/verification, retention/deletion, minors, and real-data rollout remain explicit follow-up decisions before collecting those data. Keep pilot records only as long as evaluation needs them and delete/reset the isolated pilot data before a real-data rollout. Do not treat this as an implementation of the later phone-sharing proposal.

## Event access and editing

New events start closed. HIDDEN is organizer-only except that an existing participant may still retrieve their own historical event/registration. PRIVATE is visible to members of its organization. PUBLIC can be read by an enrolled signed-in pilot account with the direct link. PUBLIC is not yet anonymous browsing/signup. Signup requires membership, visible/open signup, and a shift that has not ended. Visibility and signup are independent; hidden events cannot open signup. Payments are not involved.

Only counts and staffing descriptions appear on event pages. Names appear in one's own registrations or organizer-authorized coverage/member lists. A public event query cannot traverse into a roster because no such edge is exposed. The API does not expose contacts, credential hashes, or token hashes.

Event metadata edits use a revision check to reject stale writes. Event date/timezone are fixed once shifts exist, preventing a mismatch between the advertised event date and saved shift instants; create a new event to reschedule. Job/shift structure edits are blocked while confirmed assignments exist. Organizers must first remove affected assignments. Closing signup and editing ordinary descriptive metadata remain possible. This intentionally small policy avoids silent rescheduling; notifications and richer change management follow later. Jobs/shifts can be added or edited; deletion/archival workflows are not included in this pilot.

## Staffing invariants

Each pilot account has a distinct participant record managed by that account; dependent/group signup remains deferred. Shift capacity is 1–1000; pilot events support at most 20 jobs and jobs at most 100 shifts. Shifts start on the event date in the event's IANA timezone, last more than zero and at most 24 hours, and are stored as absolute instants. The UI converts local event-zone times and rejects ambiguous/nonexistent daylight-saving inputs. Intervals are half-open: back-to-back shifts do not overlap. No organizer conflict override is provided.

Signup and organizer assignment use the same transaction path. Event and participant row locks protect event state, capacity, and cross-event conflicts; constraints prevent duplicate participant/shift registrations. Only confirmed assignments consume capacity. UUID request IDs scope signup/assignment retry receipts to the acting user and exact action; reusing a key for another action fails. Replaying an old successful request after cancellation returns the current cancelled registration rather than taking a place again. A new signup intent needs a new request ID. Cancellation/removal are idempotent and release capacity. A later signup after cancellation creates a new registration ID while retaining the cancelled history; retrying an old cancellation cannot cancel the new booking. Event creation is not automatically retried by the client.

## GraphQL and agent access

Use schema-first Spring GraphQL at `POST /graphql`; operational HTTP diagnostics remain separate. The schema is in `src/main/resources/graphql/schema.graphqls`, with frontend operations in `frontend/src/operations.graphql`. Root operations authorize each returned graph, including nested records. PostgreSQL integration tests exercise protected traversal and concurrent mutations through GraphQL. Mutation domain errors use safe `errors[].extensions.code` values; partial data and transport status must both be checked.

Session users may issue an organization-scoped read-only or read/write agent token with at most their own current privileges. Tokens expire in seven days, are shown once, are stored only as SHA-256 hashes, and may be revoked by the owner. At most ten active tokens per user are allowed. Bearer requests do not need copied browser cookies or CSRF tokens, but only after successful token validation. Agents cannot mint/revoke tokens. Membership removal invalidates scoped token access. No OAuth scopes, federation, subscriptions, native packaging, or offline replication are introduced.

HTTP operations have a 64 KiB body limit and require Content-Length; GraphQL documents are limited to 32,000 characters, depth 12, complexity 1000, and 10-second response time. Each list has explicit bounds. Single-instance pilot rate limits are 600 GraphQL requests and 60 login attempts per remote address per minute; the reverse proxy may group several users under one address. These controls are not a distributed/global quota system. Batched HTTP GraphQL arrays are not supported. Schema introspection remains available within these limits for authorized agent discovery; it grants no business data access. Detailed server exceptions are not exposed.
