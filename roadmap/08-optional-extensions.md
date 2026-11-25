---
title: "Phase 8 \u2014 Optional extensions and later planning"
---

# Phase 8 — Optional extensions and later planning

## Outcome

Extend a useful staffing product only when deployment needs and user feedback justify the added scope. These are separate future initiatives, not one required release.

## Optional event credits

Define host-configured rates, grants, ownership, consumption points, series/occurrence behavior, refunds, and migration rules. Implement optional credit accounting with transactional and retry-safe consumption. Demonstrate that disabling credits leaves public/private event creation and staffing fully usable without a balance or checkout.

Credits may be allocated without a payment processor. Visibility, notification eligibility, and credit policy remain distinct. The historical $1-private/free-public proposal is not a global product rule.

## Optional payment adapters

Once a host actually needs purchases, define the processor contract and implement one provider adapter. Authenticate callbacks, reconcile delayed/duplicate events, and define refunds and failure recovery. Verify that alternative processors can satisfy the contract without leaking provider-specific rules into staffing. No processor is selected by this roadmap.

## Committee planning

Revisit the [historical planning design](../docs/event-planning.md) after staffing is useful. Confirm its roles and scope against the implemented organization/event model. Begin with a separately scoped task-list/assignment/completion slice, preserving separation between planning tasks and volunteer shifts. Do not revive the historical planning-first implementation sequence automatically.

## Other possibilities

Assess owner integrations such as Slack/Discord/BAND, advanced scheduling, and additional calendar features on evidence of need. Native packaging, complex offline synchronization, and broad project-management features require separate decisions and are not commitments here.

## Entry and completion criteria

Each extension requires its own stated user problem, decisions, acceptance checks, and release boundary. Core staffing and privacy must continue to work with the extension disabled. Extend portable data formats and authorization tests for every new durable capability. Payment work is never a prerequisite for the controlled pilot or internal deployment.
