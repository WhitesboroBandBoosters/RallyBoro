---
title: "Phase 3 \u2014 Broader participation and access"
---

# Phase 3 — Broader participation and access

## Outcome

Organizations can invite members and share events, and volunteers can participate through the intended public/private access paths, including account-free signup and registering others.

## Scope and sequence

1. Resolve public, private, hidden, and closed semantics independently of payment. Add organization invitations/membership management, event ownership, share links and QR codes, and organization-context sharing.
2. Implement account-free registration with secure return access, cancellation, recovery, and optional account linking. Define duplicate/overlap identity handling without exposing contact matches. Do not assume emailed links are permitted under the current notification direction.
3. Add guardian/dependent and registering-others flows, including partial-capacity behavior and authority to manage participants. Never publish minor names.
4. Resolve the phone-sharing proposal before implementing it: optional versus required disclosure, recipient scope, remembered permissions, changed managers, revocation, minors, and export implications. If adopted, show requirements before signup, record explicit consent, and apply it consistently to API and UI access. Email remains private.
5. Research notification choices for mobile and desktop, verification, invitations, and recovery. Select the minimum channels needed for the participation flows; implement any required identity/recovery delivery here. Broader app notifications follow in phase 6. Retain the preference against built-in SMS.

## Decisions and safeguards

Define invitation authority and expiration, anonymous recovery, consent retention, account linking, and behavior when notification permission is absent. Decide application authorization independently of Tailscale membership. Introduce rate limits and appropriate abuse controls before unrestricted signup or recovery endpoints are exposed.

Phone disclosure remains conditional on a product decision; unresolved policy must not produce implicit organizer access. If deferred, retain private contacts and mark disclosure as an outstanding proposal.

## Completion evidence

- Invitations and private links grant only their intended scope and handle expiry/revocation.
- Account-free participants can securely return and cancel without an unauthorized notification channel.
- Account linking, duplicate detection, and dependent management do not grant access to another person's records.
- Public and organizer responses enforce the chosen contact policy, including remembered-consent changes if implemented.
- Tests cover organization isolation, link misuse, signup retries, and unauthorized participant management; mobile and desktop walkthroughs pass.

## Dependency

Builds on phase 2. Complete the relevant identity and privacy decisions before inviting a broader audience. Provides the participation model used by phases 4–7.
