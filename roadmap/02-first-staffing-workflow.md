---
title: "Phase 2 \u2014 First staffing workflow"
---

# Phase 2 — First staffing workflow

## Outcome

An organizer creates one dated event with jobs and shifts. An invited pilot volunteer opens its link, signs up, reviews their registration, and cancels. The organizer sees coverage and can assign or remove a volunteer. This is the first useful product milestone.

## UI and API prerequisite

The user/agent business API is GraphQL, and the pilot UI must be interactive with strong typing, not a continuation of the temporary Thymeleaf/form-post shell. [Decision 002](../docs/decisions/002-graphql-and-interactive-ui.md) defines the direction. Complete [frontend/client selection #17](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/17) and [GraphQL foundation #18](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/18), coordinating identity with #10, before the first staffing slice (#11). Framework selection remains open.

## Scope and sequence

1. Integrate established authentication and the smallest useful organization/membership/organizer permission model. Bootstrap one organization and a controlled set of adult testers; use explicit membership authorization even on the tailnet. Invitation self-service follows in phase 3.
2. Implement events, jobs, shifts, participants, and registrations with stable identifiers and database migrations. Keep the participating person distinct from the account managing registration so dependents can follow without conflating identities.
3. Deliver organizer creation/editing, explicit visibility and signup controls, event links, and a coverage view through GraphQL queries/mutations and an interactive responsive UI. Start events closed until deliberately opened. Define permitted edits once registrations exist.
4. Deliver signup, personal registration review, cancellation, and organizer assignment/removal. Enforce capacity and overlap rules atomically; retries must not duplicate registrations. GraphQL clients (including agents) and the UI receive the same authorization and validation.
5. Add automated authorization, concurrent-last-slot, duplicate/retry, cancellation, and overlap tests, then walk the workflow on mobile and desktop. Verify deployment restart and restore with synthetic staffing records.

## Decisions before real pilot data

Define the minimal roles, event access states, participant identity, timezone/interval rules, and what organizer views reveal. Settle minimum contact collection and verification, retention, encryption/key recovery, and secure session handling. Prefer avoiding unnecessary contact collection; any collected contacts must satisfy the existing encryption and privacy requirements, including logs and backups. Organizer contact disclosure is not authorized by this phase.

## Completion evidence

- Two concurrent requests for the last slot yield at most one assignment; repeated submissions do not create duplicates.
- A volunteer can review/cancel only authorized registrations; an organizer cannot access another organization's data.
- Cancellation frees capacity, and organizer assignment follows the same capacity/conflict rules.
- Closed or inaccessible events reject signup server-side. Public-facing views expose no private contacts or participant roster by default.
- The full workflow is usable on mobile and desktop, survives restart, and can be exercised through the documented API.

## Boundary and dependency

Depends on phase 1. This controlled pilot requires authenticated testers; account-free access and recovery are delivered in phase 3. Waitlists, approval review, dependents, templates, payments, and outbound notifications are deferred. Pilot testers return to the application for updates. This limitation must be explicit during evaluation, not presented as the complete public product.

Review feedback here before committing to the broader release scope.

## Implementation issues

Track current progress in [initiative #2](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/2).

- [#9: docs: define controlled-pilot access, identity, and privacy rules](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/9)
- [#10: feat: integrate authentication and organization permissions](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/10)
- [#11: feat: create dated events with jobs and shifts](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/11)
- [#12: feat: browse pilot events and register for shifts](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/12)
- [#13: feat: review and cancel personal registrations](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/13)
- [#14: feat: manage coverage and organizer assignments](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/14)
- [#15: test: verify and release the controlled staffing pilot](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/15)

## Added prerequisite issues

- [#17: Select interactive typed frontend and GraphQL client](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/17)
- [#18: Establish shared GraphQL API and schema contract](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/18)
