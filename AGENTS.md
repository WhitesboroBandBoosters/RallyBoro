# RallyBoro Project Context

The initial project name is RallyBoro, under the WhitesboroBandBoosters GitHub organization. EventManager is the historical working name; the current local workspace path remains `/home/jeremy/projects/EventManager`.

## Purpose

Build a lightweight event management system, starting with volunteer staffing and signup. Committee planning and task management will follow later. Prioritize simplicity, equally important mobile and desktop experiences, privacy, minimal administration, and very low operating cost.

The active scope is volunteer staffing/signup. See docs/product-direction.md for the 2026-11-07 direction change and new product thoughts, and docs/volunteer-signup.md for signup decisions and unresolved questions. Committee planning is deferred; its prior design remains in docs/event-planning.md. Inscriva and OpenVolunteer are inspiration only, not candidate solutions to adopt.

See docs/design-brief.md for the shareable product/design overview and proposed broad architecture. It distinguishes requested capabilities, proposed delivery scope, and open decisions.

See roadmap/README.md for the proposed implementation initiatives and release boundaries. Prioritize the runnable foundation and controlled staffing pilot in phases 1–2; later initiatives are not authorization to implement the entire roadmap at once. Resolve each phase's blocking decisions before implementation.

## Current direction (supersedes earlier planning-first assumptions below)

- API first and agent friendly; apply the same authorization and privacy boundaries to agents and the UI.
- Support import/export to move personal and organization data between independently hosted deployments, including previous history and templates. Preserve relationships and contact privacy during migration; organization exports must not reveal contacts outside authorized scope; whether the proposed phone-sharing consent permits exports remains unresolved. Transfer format, permissions, identity mapping, and encryption/key handling remain to be designed.
- Mobile and desktop experiences are equally important for volunteer signup and organizer workflows; neither is secondary. Research notification options later; no additional notification channel is selected by this requirement.
- Support calendar integration with Apple Calendar, Microsoft calendars, and Google Calendar, including ICS/iCalendar compatibility. Download, subscription, and direct-provider integration scope remains to be designed; this does not require calendar views or two-way synchronization.
- Distinguish reusable event templates, event series grouping related occurrences, and individual dated events. A series can be extended with additional events. Each event starts from the initial template or first event and can have its own shifts/capacities and other adjustments. New series events default to hidden with signups disabled. See docs/volunteer-signup.md for details and open design questions.
- Separate core event capabilities from deployment pricing. Most internal deployments should work without credits, checkout, or a payment processor. Event credits are optional; hosts define their own rates and may bring their own payment processors. Credit rules and processor contracts remain to be designed.
- Earlier free-public/no-notifications and $1-private pricing are a proposed policy for a particular hosted offering, not universal product rules. Define notification eligibility as deployment policy with shared privacy and abuse protections; private visibility must not inherently require payment. Organizers can share links/QR codes or share with an organization they manage.
- Organizations support invitations, membership, and events within an organization context. Organizational authorization is required in the initial scope, even though full tenancy machinery remains premature.
- Contact information remains private by default and encrypted at rest. Stakeholder feedback (2026-11-19) proposes an exception for phone numbers: voluntary disclosure to event managers, event-required disclosure clearly presented before signup, and an optional remembered permission for the same organizer. Scope, revocation, exports, and treatment of minors remain unresolved; do not assume general organizer access or permission to share email addresses.
- The current notification preference is app-only notifications in the short term, with no built-in direct texting. This is a tentative direction replacing the earlier text/email delivery proposal, not a selected app/push technology. Verification, account-free signup, desktop access, and abuse controls still require design.
- Support requested approval-required shifts: pending requests do not reserve capacity; volunteers can see a pending-request count and still apply. Only approved assignments consume capacity. Approval must enforce capacity and conflict rules; qualification-review details and interaction with waitlists remain open.
- Slack/Discord integrations for event owners are possibilities, not MVP commitments. BAND API findings are recorded in docs/product-direction.md.
- Publish the code as open source; Apache-2.0 or MIT are candidates, with no license selected yet.
- Prototype direction (2026-11-25): use Java and Spring Boot, reflecting the maintainer's strong familiarity with both. The build foundation targets Java 25 with Spring Boot 4.2.0 dependency management and Maven 3.10.1 via the checked-in Maven Wrapper; use `./mvnw` for builds. Near-term hosting will be on the maintainer's own infrastructure with access through Tailscale. PostgreSQL is selected for the first implementation. The foundation uses JDBC/Flyway, Spring MVC/Thymeleaf, and Docker Compose application packaging. Spring Security OIDC is selected for initiative 2. See docs/decisions/001-foundation.md and docs/running.md. Database deployment and all real credentials must remain outside project commits. Managed-cloud hosting and scale-to-zero are not prototype prerequisites. Work through product behavior first; another implementation language can be considered after prototyping, without treating a rewrite as required. Earlier stack choices below are historical candidates, not current defaults. Evaluate remaining choices against signup, privacy, portability, notifications, optional payments, and abuse controls.
- The historical planning domain and staged plan below are retained for later reference, not the active MVP implementation order. Define a staffing/signup MVP before substantial application code.

This repository is at its bootstrap stage, with the runnable initiative-1 foundation but no event staffing functionality yet. Treat this document as product and architecture context, not permission to implement the entire roadmap at once.

## Earlier planning product goals (retained for later)

- Support an organization, administrators, event owners, committee members, reusable event templates, event instances, planning task lists, individual task assignment, and useful personal views.
- Deliver one responsive application for desktop browsers, mobile browsers, and installed PWAs. Mobile task review and completion are first-class workflows.
- Authenticate with Google while keeping authorization and organization membership inside the application.
- Support in-app notifications, transactional email, attachments, and later Web Push where it proves useful.
- Avoid an always-running server and remain near $0/month at small nonprofit usage. Domain registration is outside this target.
- Keep external services behind clear interfaces so they can be replaced if needs or pricing change.
- Apply YAGNI aggressively; do not recreate Jira, Asana, or Monday.

## Historical architecture candidates (not selected)

- **Frontend:** React + TypeScript, built as a responsive installable PWA.
- **Runtime/API:** Cloudflare Workers using its scale-to-zero model.
- **Relational data:** Cloudflare D1, with automated migrations. The expected scale and concurrency are modest.
- **Attachments:** Cloudflare R2 is reserved for future file attachments; initial planning uses external reference links.
- **Authentication:** Google OAuth through Better Auth, or the current well-supported equivalent if ecosystem review shows a better fit for Workers and D1. Never implement OAuth/OIDC manually. Store application users/sessions in D1.
- **Email:** Resend initially, behind an application-owned email interface.
- **Hosting:** An all-serverless Cloudflare deployment targeting free-tier allowances and near-$0 operation.

The list above preserves the earlier proposal; none of these technologies or providers is currently selected. Earlier fallback candidates were PostgreSQL via Supabase or Neon and Cloud Run plus Neon. Do not treat those as selected alternatives either. Evaluate a simple operating model against the current product requirements and document the eventual choices. Do not use experimental Lakebed as the production foundation.

## Earlier planning identity and authorization

Google proves identity; EventManager grants access. A Google account alone must not authorize membership. Start with invitation/allowlist membership and the smallest useful permission model. Use administrator, event owner, committee member, and viewer roles as defined in docs/event-planning.md. Bootstrap administrators from a comma-separated deployment allowlist of Google email addresses.

Although the initial deployment serves one nonprofit, avoid schema choices that make multiple organizations impossible. Do not build full multi-tenancy machinery prematurely.

## Later planning domain

The core relationship is:

`Organization -> Event Template -> Event Instance -> Planning Task Lists -> Tasks`

Tasks have a simple section, status, zero or one assignee, comments, titled reference links, creator, ordering, recoverable removal, and audit timestamps. They deliberately omit due dates, priorities, dependencies, subtasks, multiple assignees, and initial file attachments.

Useful eventual views include My Tasks, Events, Completed, and Unassigned.

## PWA and mobile policy

Give mobile and desktop equal importance within an online-first experience with graceful temporary-connectivity handling and deep links. The earlier installable-PWA proposal included basic static-asset caching, a manifest/icons, and standalone presentation; validate those implementation choices when selecting technology. Research notification options later rather than treating Web Push as already selected.

Do not build a complex offline-first replicated-data or synchronization system initially. Do not create a separate native Android application. Keep the frontend amenable to later Capacitor packaging only if native features become a real requirement.

## Engineering principles

- Prefer straightforward implementation, clear data relationships, simple deployments, explicit service boundaries, and maintainability over cleverness. Earlier TypeScript and relational database proposals remain technology candidates.
- Make local development resemble production and test business and authorization rules automatically.
- Never expose privileged persistence operations or authorization decisions directly to the client, regardless of the eventual storage technology.
- Avoid microservices, Kubernetes, premature abstraction, premature multi-tenancy, custom auth protocols, and speculative offline synchronization. A straightforward self-hosted application is acceptable for the prototype; the earlier preference to avoid always-running compute does not block this direction.
- Do not add a paid dependency merely for convenience when a maintainable free option meets the need; likewise, do not add major complexity merely to avoid a hypothetical small future charge.

## Historical planning-first vertical-slice plan (superseded)

Before substantial application code, review the architecture against the current platform and record the selected framework/tooling, repository layout, initial D1 schema, authentication integration, local workflow, deployment workflow, and incremental plan.

Then implement one end-to-end production-capable slice in stages:

1. Establish the React/TypeScript PWA and local Cloudflare Workers development skeleton.
2. Add D1 bindings and an automated migration workflow.
3. Integrate Google sign-in using Better Auth or the validated current equivalent.
4. Add the minimal User, Organization, Membership, Event Template, Event Instance, Event Owner, Planning Task List, and Task data model.
5. Demonstrate authenticated event/task CRUD through the Worker to D1, including create, edit, and complete.
6. Deploy to Cloudflare and verify the complete production authentication and authorization flow.
7. Review the implementation and architecture before expanding scope.

The first milestone is deliberately small:

`Google Login -> Organization -> Event -> Planning Task -> Assign / Edit / Complete`

After that milestone, expand in thin vertical slices: task lists and assignments; personal task views and due dates; comments and reference links; Resend invitations and assignment notifications; Web Push; then only evidence-backed later features.

## Deferred possibilities

Committee planning is deferred in docs/event-planning.md. Independently recurring tasks, subtasks/dependencies, calendar views, rich notification preferences, mentions, audit history, advanced search/filtering, native packaging, strong offline editing, and full multi-organization tenancy machinery remain deferred. Avoid blocking them unnecessarily, but do not implement them without an approved requirement. Basic organization membership, invitations, and organization-scoped events are now part of the signup direction. Apple/Microsoft/Google calendar integration and ICS/iCalendar support are requested capabilities; their delivery scope remains to be defined.
