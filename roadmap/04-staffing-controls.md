---
title: "Phase 4 \u2014 Staffing controls"
---

# Phase 4 — Staffing controls

## Outcome

Organizers can handle restricted roles, excess demand, and changes without misleading volunteers or overfilling shifts.

## Scope and sequence

1. Add approval-required shifts and review permissions. Pending applications consume no capacity; show pending counts separately from assignments and remaining slots. Applicants must see that a request is not a confirmed place.
2. Add approve, decline, and withdraw transitions with suitable review history. Approval atomically enforces capacity and overlap rules, including competing manager actions.
3. Add capacity waitlists distinct from qualification review. Resolve promotion ordering, manual versus automatic promotion, offers/expiry, and behavior when a pending applicant reaches a full shift. Any offer mechanism requiring delivery depends on the selected notification capability.
4. Define and implement changes after signup: capacity reductions, rescheduling, approval-policy changes, event cancellation, and organizer reassignment. Make affected status visible even when outbound notifications are unavailable.

## Decisions before implementation

Settle qualification evidence and retention, reviewer authority, applications after a shift fills, pending conflicts, conflict overrides, back-to-back intervals, and waitlist/pending interaction. Do not assume automatic qualification verification or first-come approval.

## Completion evidence

- Competing approvals cannot exceed capacity; a pending application never blocks another eligible signup.
- Counts, statuses, and permissions agree in API and UI.
- Waitlist promotion is retry-safe and cannot bypass qualification or overlap checks.
- Cancellation and edits cannot silently leave impossible assignments; affected users can discover their current status.
- Automated transition/concurrency tests and mobile/desktop review flows pass.

## Dependency

Builds on phase 3's identity/participant model. Can precede or follow phase 5 after pilot review. Integrates with phase 6 for eligible notifications; do not introduce SMS or assumed email delivery to make waitlists work.
