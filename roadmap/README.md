---
title: "RallyBoro implementation roadmap"
---

# RallyBoro implementation roadmap

Proposed delivery plan · November 25, 2026

This roadmap translates the [design brief](../docs/design-brief.md) into ordered initiatives. It proposes release boundaries rather than declaring unresolved product choices settled. The first goal is a working application on the maintainer's infrastructure, accessible through Tailscale, followed by a small usable staffing workflow. Dates and effort estimates are intentionally omitted until each initiative is broken into implementation tasks.

Java 25, Spring Boot 4.2.0, and the Maven Wrapper are the current build foundation. Docker is available. Database, frontend, authentication, and application deployment details still need decisions. Keep one application with clear business boundaries, a documented API, and equally usable mobile and desktop workflows. A later language rewrite is possible, not a prerequisite or planned milestone.

## Initiative sequence

| Phase | Initiative | Deliverable |
| --- | --- | --- |
| 1 | [Runnable foundation](01-runnable-foundation.md) | Application and persistent storage running through Tailscale, with a minimal responsive page and health check. |
| 2 | [First staffing workflow](02-first-staffing-workflow.md) | An organizer creates an event; a pilot volunteer signs up, reviews, and cancels; the organizer manages coverage. |
| 3 | [Broader participation](03-broader-participation.md) | Invitations, private/public access, account-free participation, dependents, and a resolved contact/notification policy. |
| 4 | [Staffing controls](04-staffing-controls.md) | Approval-required roles, waitlists, and safe management of changes after signup. |
| 5 | [Templates and series](05-templates-and-series.md) | Reusable staffing structure and independently adjustable occurrences. |
| 6 | [Notifications, calendars, and outputs](06-notifications-calendars-outputs.md) | Selected app notifications, calendar output, and useful operational reports. |
| 7 | [Independent deployment and portability](07-deployment-and-portability.md) | Documented installation, upgrades, recovery, and verified personal/organization migration. |
| 8 | [Optional extensions](08-optional-extensions.md) | Separately scoped event credits, payment adapters, and later committee planning. |

Phases 1–2 are the initial implementation priority. Review the prototype after phase 2 before expanding. Phase 3 builds on phase 2; phases 4 and 5 build on phase 3 and can be reordered according to pilot feedback. Notification research starts in phase 3; phase 6 delivers the selected channels. Phase 7 starts operational groundwork in phase 1 and completes migration after the relevant domain features exist. Phase 8 has no dependency on finishing every optional phase-6 integration.

## Release boundaries

- **Running prototype — phase 1:** demonstrate startup, deployment, persistence, and responsive access with synthetic data. This is an infrastructure milestone, not a usable staffing product.
- **Controlled pilot — phase 2:** one organization and invited, authenticated adult testers can complete the core staffing workflow. Account-free signup, dependents, waitlists, and approvals remain explicitly deferred. Use synthetic data until the phase-2 access and contact safeguards pass.
- **Broader volunteer release — phases 3–6:** proposed default scope includes the requested participation, staffing, reuse, basic calendar, and notification capabilities. Reassess the exact release checklist after the controlled pilot; any deferred requested capability stays visible in this roadmap. Public deployment also requires the phase-7 operational checks applicable to that host.
- **Independent-host release — phase 7:** another operator can install, restore, and migrate representative personal and organization records. Public source availability alone does not mean these capabilities are finished.

Tailscale access does not replace application authorization and does not settle eventual public event access. No phase requires credits, checkout, or a payment processor. Privacy, capacity correctness, and organization isolation apply from their first relevant slice.

## How to execute an initiative

Before starting, resolve only the decisions blocking that initiative and record them in the product documents or a focused architecture decision record. Break the initiative into small, demonstrable API/UI/persistence slices. Add automated business-rule and authorization tests alongside those slices, then verify the user flow on mobile and desktop. Record feedback and update this roadmap before moving on. Do not implement every initiative as one change.

## Source coverage

The [design brief](../docs/design-brief.md), [signup requirements](../docs/volunteer-signup.md), and [product direction](../docs/product-direction.md) remain authoritative. Phone disclosure is still a proposal. App-only notifications are a tentative preference. Optional integrations and the [historical planning design](../docs/event-planning.md) remain future work. This roadmap preserves those distinctions.
