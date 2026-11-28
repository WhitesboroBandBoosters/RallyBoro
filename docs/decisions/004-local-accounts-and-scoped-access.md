---
title: Local accounts and scoped access
---

# Local accounts and scoped access

November 28, 2026 · Phase 3, first implementation slice · Issue #20

## Delivery boundary

Deliver local account registration/login, password change and offline recovery, self-service organization creation, organization membership/permission management, event-specific organizer grants, and shareable invitations. The business API remains GraphQL; Spring Security manages browser login/logout and CSRF. No external identity or email provider is required. Keep the controlled tailnet deployment and existing pilot records working.

Guest participation/recovery, dependents, phone sharing, automated email, proximity discovery, and public anonymous event browsing remain later slices with their open decisions visible in phase 3. The email research thread stays context-only. This slice does not implement the whole initiative.

## Accounts and identity

Users choose a username, display name, and password; no phone/email is collected. New users receive random stable IDs, independent of their login method. Credentials and external issuer/subject mappings are separate from the user and domain records. Preserve existing pilot IDs/history during migration. Use Spring Security BCrypt, bounded passwords of 15–72 UTF-8 bytes, and login/registration/recovery limits. Passwords are hashed, never encrypted for retrieval. Never log credentials, invitation tokens, or recovery keys.

On registration, show a random offline recovery key once, storing only its hash. Recovery requires the username and that key; consume/rotate the key atomically, invalidate existing local-password sessions and agent tokens, and show the replacement once. Password changes require the current password and also invalidate local sessions/tokens. A signed-in local user can replace a lost recovery key by proving their current password; this also lets imported pilot users establish a key. There is no administrator reset endpoint that silently takes over another user's account. Securely store the recovery key outside the browser; losing both password and key has no self-service recovery in this slice.

Pilot enrollment is a compatibility import, not the normal product onboarding flow. Import configured pilot credentials and membership once without resetting later changes on restart. OIDC remains optional for previously enrolled identities; multiple-provider linking UI follows later and must prove control rather than merge by matching email/display names.

## Scoped permissions

Any signed-in browser user can create an organization and gets management permission within it. No organization bootstrap is required. Memberships hold two small permission grants: manage the organization (including invitations and permissions) and create/manage all its events. Organization management implies event management. Ordinary members can participate in organization events. An individual member may instead receive management of specific events without authority over other events or organization membership. These are contextual grants, not global account roles. Retain legacy role fields only for compatibility and migrate pilot organizer memberships to management grants.

Only organization managers can grant/revoke organization permissions or event-manager grants. Serialize permission changes on the organization, preserve at least one manager, invalidate a removed member's scoped agent tokens, and check membership on every protected action. Membership removal retains registration history and permits authorized self-cancellation. Events cannot move organizations. Agent credentials retain organization/read-write scope and cannot create organizations or accept transferable invitations; membership/invitation administration is browser-session-only in this first slice.

## Invitations

An organization manager creates a single-use, seven-day bearer invitation granting membership and optionally event-wide or organization-management permissions, or management of one event. No contact address is required or exposed. The organizer shares the link through a channel of their choice. Show only a generic invitation screen before sign-in; do not expose the organization's name, members, or event details to an unauthenticated token lookup. After sign-in, preview the organization and granted scope and require explicit acceptance. Tokens are 256-bit random values; persist only a hash. Return the link token once, keep it out of list responses/logs/referrers, support revocation, and serialize acceptance with permission changes. An invitation is valid only while its creator remains an organization manager. Existing membership permissions are never reduced by accepting an invitation. Repeated acceptance by the same account is idempotent; another account cannot reuse it.

List members/invitations with explicit pagination, bound outstanding invites, and keep the UI usable for large lists. Audit organization creation, invitations, membership/permission changes, and event grants without recording secrets. Read-only agents cannot mutate. Event-specific managers can inspect assignment candidates and coverage only within their permitted organization/event boundaries.

## Verification

Use PostgreSQL Testcontainers for signup identity, password/recovery rotation and invalidation, invitation races/expiry/revocation, least privilege, last-manager preservation, removed-member behavior, and event/organization/agent isolation. Exercise create account → create organization → share/accept invitation → manage membership/event access on desktop and narrow/mobile viewports against a disposable app/database. Existing staffing tests must remain green. No automated suite seeds the shared deployment.
