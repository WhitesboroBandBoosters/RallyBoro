---
title: Initiative 3 — first slice verification
---

# Initiative 3 — accounts and scoped access

November 28, 2026 · First slice · Issue [#20](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/20)

## Delivered scope

Local account creation/login, password changes, offline recovery-key rotation and password recovery, self-service organization creation, scoped organization/event permissions, member management, single-use expiring/revocable invitations shared manually, and an access-history view. Account creation does not imply organization membership. The same account can manage one organization, organize one event elsewhere, and volunteer in another. There is no organization bootstrap or required identity/email provider.

The React/TypeScript client uses generated GraphQL operations for accounts, organizations, permissions, and invitations. Spring Security still manages browser session login/logout and CSRF. The interface includes no-organization onboarding, invitation preview/acceptance after sign-in, organization switching, member/invitation/event-grant pagination, account settings, and permission-aware event controls. Accounts with no current organization can still inspect/cancel their own registration history and view authorized event deep links.

[Decision 004](decisions/004-local-accounts-and-scoped-access.md) records the limits and [the running guide](running.md) covers operation and recovery. The old pilot enrollment file is an optional one-time compatibility import, with an empty non-credential placeholder for new installations. Existing user IDs, memberships, staffing records, and history remain intact. Imported account changes are not reset on restart. Normal accounts use random stable IDs with separate credential/external-identity tables. Local-to-OIDC linking UI remains future work.

## Verification evidence

- Java verification passes 35 tests with no failures/skips: 34 PostgreSQL Testcontainers integration tests and one architecture check that rejects direct JDBC/text-query persistence in application sources. Thirteen new cases cover account identity, case-insensitive username uniqueness, password limits/hashing, CSRF, account-request limits/disabled mode, recovery-key replacement and concurrent consumption, session/token invalidation, invitation acceptance races/expiry/revocation, issuer permission loss, last-manager concurrency, event-specific access, removed-member history/cancellation, organization isolation, and browser-only administration boundaries.
- Four Playwright runs pass: the existing staffing workflow and the accounts/organization workflow, each at 1440×1000 and 375×800. Mobile contexts enable mobile/touch emulation. New flows cover account/key onboarding, empty-membership registration access, creating a separate organization as an ordinary member, invitation privacy before sign-in and explicit acceptance, scoped permission changes, last-manager rejection, invitation revocation, password change, recovery, and preserved organization access afterward. These are Chromium emulated viewports, not physical-device/WebKit/Firefox verification.
- The container image builds. A protected pre-upgrade backup was restored into a separate temporary database, and the new image applied V4 successfully with readiness UP. It retained two events, three user IDs, and three registration records, imported three legacy credentials, and recorded their one-time import markers. A restart preserved deliberately changed display name, permission, and credential values in that disposable clone, confirming import does not reset them.
- After deployment, readiness is UP and read-only browser checks pass for the existing organizer, volunteer, and helper accounts on desktop/mobile. The live database still has two events, three users, and three registration-history records after applying V4; no new business records were seeded. The temporary upgrade-check container/database were removed, while the protected backup remains outside the checkout.
- Automated suites use disposable application/database environments. The shared pilot's two existing private/hidden events are reused, not duplicated. No tests create new business records there.

## Persistence refactor

[Decision 005](decisions/005-jpa-persistence.md) replaces all handwritten application JDBC queries with Spring Data JPA/Hibernate entities, derived repositories, and generated-metamodel specifications. GraphQL contracts, IDs, explicit revision/locking behavior, and Flyway migrations are retained. Hibernate validates the existing schema; Open Session in View is disabled. The independent JDBC-based fixture/assertion code in integration tests remains intentionally separate from ORM persistence. Full Maven verification passes with the ORM, and all four desktop/mobile browser runs pass. The new image validated the existing four migrations on a protected restored copy and loaded its two events, three users, and three registration-history records through read-only persona checks. No schema migration was added. After deployment, readiness and read-only persona checks also pass against the live pilot; record counts and migration count remain unchanged. The temporary restore-check database/container were removed, and the backup remains protected outside the checkout.

## Remaining phase-3 work

This is not completion of the whole initiative. Guest signup/recovery and optional account linking, dependent participation, phone-sharing policy and contact protection, notification/email delivery decisions, anonymous public browsing, and proximity-based public-event discovery remain separately scoped work. Google provider setup remains [#19](https://github.com/WhitesboroBandBoosters/RallyBoro/issues/19). The email-provider context thread remains parked.

The deployment remains tailnet-only. Email/phone collection and real-data broader rollout policies are not established by these account and permission changes. Database deployment artifacts, credentials, backup files, recovery/invitation values, generated assets, and test artifacts are excluded from commits.
