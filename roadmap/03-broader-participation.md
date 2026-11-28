---
title: "Phase 3 \u2014 Broader participation and access"
---

# Phase 3 — Broader participation and access

## Outcome

Any signed-in user can create an organization, invite others with scoped permissions, and share events. Volunteers can participate through the intended public/private access paths, including account-free signup and registering others. Proximity-based public-event discovery is also requested; its delivery boundary remains to be defined.

## First implementation slice — local accounts and scoped access

Issue [#20](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/20) delivers local accounts and offline recovery, self-service organization creation, scoped organization/event grants, member management, manually shared expiring invitations, and access history. See [decision 004](../docs/decisions/004-local-accounts-and-scoped-access.md). It requires no OAuth/email provider. Existing pilot IDs/history are preserved; operator enrollment becomes a one-time compatibility import.

This starts phase 3; it does not complete it. Guest participation and recovery, dependents, phone-sharing policy, email/channel selection, public anonymous browsing, and proximity discovery remain separate follow-ups. Self-service provider linking will use the stable account/identity separation; no automatic linking by email/name is allowed. Further issue breakdown follows the unresolved decisions rather than silently implementing them.

## Organization creation and permissions

Direction clarified November 28, 2026. Organization creation is a normal signed-in user action. The creator receives permission to manage that organization and invite others; no operator-created organization, administrator allowlist, or organization bootstrap command is required.

Authorization belongs to the relevant organization or event, not a global user category. The same account can manage one organization, organize a specific event elsewhere, and volunteer at another event. Grants distinguish organization management, invitation/permission management, and event management. Exact grant names, inheritance, delegation limits, and ownership-transfer behavior still need a small implementation design; this direction does not require a general-purpose permissions framework.

Authentication alone grants no access to existing organizations. Invitations confer only their specified scope, and users cannot delegate authority they do not hold. Public/guest event participation remains a separate access path. Evaluate permissions consistently in the GraphQL API and UI.

Use the normal account and organization-creation flows for a new deployment. Infrastructure configuration and emergency recovery remain operator concerns; an installation-wide administrator account is not a prerequisite for organization workflows. The pilot's file-based enrollment and ORGANIZER/VOLUNTEER membership values describe the earlier phase-2 model; the first phase-3 slice retains legacy fields/import for compatibility and uses scoped permissions for authorization.

## Scope and sequence

1. Resolve public, private, hidden, and closed semantics independently of payment. Add signed-in organization creation, organization/event permission grants, invitations/membership management, event ownership, share links and QR codes, and organization-context sharing.
2. Implement account-free registration with secure return access, cancellation, recovery, and optional account linking. Define duplicate/overlap identity handling without exposing contact matches. Do not assume emailed links are permitted under the current notification direction.
3. Add guardian/dependent and registering-others flows, including partial-capacity behavior and authority to manage participants. Never publish minor names.
4. Resolve the phone-sharing proposal before implementing it: optional versus required disclosure, recipient scope, remembered permissions, changed managers, revocation, minors, and export implications. If adopted, show requirements before signup, record explicit consent, and apply it consistently to API and UI access. Email remains private.
5. Research notification choices for mobile and desktop, verification, invitations, and recovery. Select the minimum channels needed for the participation flows; implement any required identity/recovery delivery here. Broader app notifications follow in phase 6. Retain the preference against built-in SMS.

## Local public-event discovery

Requested November 28, 2026. Add discovery of local public events by proximity to a location as a planned slice; agree its release boundary alongside the participation/access work rather than treating it as already delivered or a prerequisite for the first accounts/organization slice.

- Provide a GraphQL search and responsive results so people can find public events without prior organization membership or a known event link.
- Decide location input, radius/units, date filters, distance ordering, venue coordinates/geocoding, and handling of events without a usable location. A map is not required by this request.
- Resolve whether every public event is discoverable or whether public-but-unlisted links need separate treatment. Keep publication, discoverability, and signup-open state distinct.
- Exclude private/hidden events from results, counts, and location metadata at the server. Discovery must not expose participant details or change event permissions. Pilot fixtures must remain private/hidden.
- Define user-location handling and published venue precision before choosing a location/geocoding provider. Device location is only one candidate input; persistent location tracking is not selected.
- Decide anonymous browsing and current-deployment versus cross-deployment scope. Do not assume a federated directory.

When this slice ships, verify radius/date boundaries, unusable locations, private/hidden exclusion, and consistent API/UI behavior on mobile and desktop. These are proposed acceptance areas, pending the initial search design.

## Decisions and safeguards

Define invitation authority and expiration, anonymous recovery, consent retention, account linking, and behavior when notification permission is absent. Decide application authorization independently of Tailscale membership. Introduce rate limits and appropriate abuse controls before unrestricted signup or recovery endpoints are exposed.

Phone disclosure remains conditional on a product decision; unresolved policy must not produce implicit organizer access. If deferred, retain private contacts and mark disclosure as an outstanding proposal.

## Completion evidence

- A signed-in user can create an organization and manage/invite within it without operator setup; doing so grants no access to another organization.
- The same account can hold different permissions across organizations and events. Invitation and permission changes cannot escalate beyond the grantor's authority.
- Invitations and private links grant only their intended scope and handle expiry/revocation.
- Account-free participants can securely return and cancel without an unauthorized notification channel.
- Account linking, duplicate detection, and dependent management do not grant access to another person's records.
- Public and organizer responses enforce the chosen contact policy, including remembered-consent changes if implemented.
- Tests cover organization isolation, link misuse, signup retries, and unauthorized participant management; mobile and desktop walkthroughs pass.

## Dependency

Builds on phase 2. Complete the relevant identity and privacy decisions before inviting a broader audience. Provides the participation model used by phases 4–7.

## First-slice evidence

See [initiative 3 verification](../docs/initiative-3-verification.md) for delivered account/organization behavior, isolated integration/browser coverage, migration checks, and remaining scope. Issue #20 stays open until its implementation PR merges. Merge the phase-2 PR before the dependent phase-3 PR.
