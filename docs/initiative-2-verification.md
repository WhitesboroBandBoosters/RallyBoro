---
title: Initiative 2 verification
---

# Initiative 2 verification

November 27, 2026 · Controlled staffing pilot · PR #16

## Delivered product

React/TypeScript client with organization navigation, event cards, event/job/shift editor, volunteer signup, My registrations and cancellation, organizer coverage/assignment/removal, and scoped agent-token management. Client operations/types come from the schema-first Spring GraphQL API. PostgreSQL migrations retain users, organization permissions, events, jobs, shifts, participant identities, registration attempts/history, retry receipts, and token hashes. No phone/email/contact fields or secrets are exposed.

The [pilot decision record](decisions/003-staffing-pilot.md) defines the selected framework/client, enrollment/authentication options, visibility, edit restrictions, concurrency rules, and deferred capabilities. The [API guide](api.md) and [running guide](running.md) describe use and operations.

## Verification evidence

- Maven runs locked frontend dependency installation, validates checked-in GraphQL operations, generates operation types, performs strict TypeScript checks, builds production assets, compiles Java, and executes PostgreSQL Testcontainers integration tests.
- Nineteen Java integration tests pass with zero failures or skips. Backend cases cover competing GraphQL requests for the last place, cross-event overlap, back-to-back intervals, retry receipts, cancellation/capacity release, assignment/removal, closed signup, stale revisions, edits with existing assignments, organization/roster privacy, CSRF, schema validation/limits, anonymous access, bearer scope/revocation, and delayed cancellation after a new registration attempt.
- Playwright runs the packaged React client against a separate test-only Spring host and disposable PostgreSQL database, with independent organizer/volunteer sessions on 1440×1000 and 390×844 viewports. It creates an event with two jobs and shifts, reloads persisted data, opens signup, signs up another account, verifies coverage, cancels, and verifies freed capacity. Requests must complete before subsequent destructive navigation; test traces/videos are disabled.
- The same workflow is exercised over the actual tailnet HTTPS endpoint with randomly generated, externally stored synthetic pilot credentials. Screenshots are ignored local QA artifacts; tests assert no horizontal overflow. These are emulated viewports, not physical-device verification.
- The application image runs non-root, binds host loopback 8088, and is served on the existing tailnet-only HTTPS port 8443. PostgreSQL deployment and credentials remain independently managed and outside commits.
- Restart, schema validation, and a protected backup/restore check verify durable staffing records and membership relationships. Restored data is checked in a separate empty database before any configuration switch. CI repeats Java/database tests, browser flows, and the application image build.

## Release limits and follow-up

The local pilot uses explicit Spring Security password login for synthetic accounts, off by default. Google OIDC integration is wired, but provider credentials and a live Google round trip still require operator configuration and verification, tracked in [issue #19](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/19). Do not claim real-provider acceptance from the synthetic login tests.

Guest access, invitations, dependents, waitlists, approval-required shifts, templates/series, app notifications, contact sharing/verification/encryption policy, payments, and full deployment migration remain subsequent initiatives. PUBLIC event viewing still requires an enrolled signed-in pilot account. No real personal/contact data or minors' records should be introduced until the corresponding privacy and participation policies are settled.

Issue #15's automated demonstration is complete when the PR checks pass; stakeholder pilot feedback remains to be collected. PR #16 remains the review/merge boundary. This document records capabilities and test scope, not approval for an unrestricted public launch.
