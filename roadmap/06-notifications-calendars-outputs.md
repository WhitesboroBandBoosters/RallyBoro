---
title: "Phase 6 \u2014 Notifications, calendars, and operational outputs"
---

# Phase 6 — Notifications, calendars, and operational outputs

## Outcome

Volunteers can keep track of commitments, and organizers can use staffing information during an event.

## Scope and sequence

1. Deliver the app-notification approach selected during phase 3 research. Cover the chosen assignment, cancellation, approval, waitlist, and event-change triggers. Define desktop access and a visible in-app status path for people who cannot receive notifications. Native/PWA/Web Push choices remain open until that research concludes.
2. Implement authorized, retry-safe background delivery, preferences, deployment eligibility policy, and abuse limits. Protect delivery identifiers and notification content. Notification permission does not authorize organizer contact disclosure.
3. Start calendar delivery with downloadable ICS/iCalendar as the proposed smallest slice. Validate representative Apple, Microsoft, and Google calendar imports, timezones, event identifiers, and privacy. Describe download/update limitations clearly.
4. Add essential coverage/roster and printable output. Prioritize check-in sheets, CSV, and hours reporting from pilot evidence; decide scheduled versus actual hours before recording hours. Enforce contact/participant permissions for every export.
5. Evaluate subscription feeds, add-to-calendar links, and direct-provider integrations as separate follow-up slices. Decide private feed revocation, updates, and cancellations before subscriptions ship. Do not imply two-way synchronization.

## Decisions before implementation

Confirm supported notification clients, denied-permission behavior, delivery/retry semantics, reminder scope, retention, calendar scope, and essential reports. Select which calendar enhancements are necessary for the first broader release. Phone-sharing consent must not silently permit export.

## Completion evidence

- Only eligible authorized recipients receive the selected messages; retries do not duplicate business actions.
- Failure or denied notification permission leaves users able to inspect current application state.
- Calendar output imports correctly in supported clients without private contact leakage; update limitations are documented.
- Reports reconcile with assignments and permissions on mobile and desktop.

## Dependency

Uses phase 3's access/channel decisions and adds triggers as phases 4–5 ship. Basic ICS and coverage reports may be pulled forward after phase 2 if useful. Slack, Discord, and BAND integrations remain exploratory.
