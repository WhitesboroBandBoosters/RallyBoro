---
title: "RallyBoro \u2014 Product and Architecture Design Brief"
---

# RallyBoro — Product and Architecture Design Brief

**Working draft 0.6 · November 25, 2026 · For discussion**

RallyBoro helps organizers staff community events and helps volunteers find and manage shifts, with equal importance given to mobile and desktop experiences. It is designed for occasional use, low operating cost, private contact information, and deployment in infrastructure chosen by the operator.

The first product focus is **volunteer staffing and signup**. Committee planning and task management are a later expansion. This brief consolidates the current direction into a standalone review document. Requested capabilities are distinguished from proposed delivery scope and unresolved decisions. Java and Spring Boot are the prototype direction; the remaining stack choices and first-release feature boundary are open.

## 1. Purpose and audience

Small nonprofits and community groups need to organize staffing without expensive per-user subscriptions or complex project-management tools. Organizers need a clear picture of available shifts and remaining capacity. Volunteers need a simple way to participate, cancel, and return to their commitments.

The intended experience is useful on its own for one event, supports organizations running related events, and retains reusable structure and history over time. The source will be open, and people must be able to move their data between independently operated deployments.

Primary participants are volunteers, people registering on behalf of others, event organizers, organization managers/members, and deployment operators. Automated clients and agents are also supported clients, subject to the same permissions and privacy rules as people. Exact role names and permissions remain to be defined for the signup product.

## 2. Project principles

1. **Mobile and desktop are equally important.** Signup and organizer workflows must work well on both. Adapt layouts and interactions to each device without treating either experience as secondary.
2. **API first and agent friendly.** Core actions should be available through a documented application API. The interface and automated clients use the same business rules and authorization boundaries.
3. **Contacts are private by default.** All contact information is encrypted at rest. Stakeholders propose a scoped exception for volunteer-authorized phone sharing, including an event-required disclosure option and remembered permission for the same organizer. This policy is under discussion; it does not grant unrestricted access or permission to disclose email addresses. APIs, reports, integrations, and migration tools must enforce the eventual consent boundaries.
4. **Communication is controlled and accountable.** The current preference is app-only notifications in the short term, without built-in direct texting. This is tentative and does not select an app or push technology. Contact disclosure and notification permission are separate, and abuse controls remain necessary.
5. **Simple, economical operation.** Minimize administration and ongoing infrastructure cost. Core event functionality must work without a payment system. Hosts may optionally use event credits and choose their own rates and payment processors; delivery and abuse controls remain necessary regardless of pricing.
6. **Open source and portable data.** Support independent deployment and practical migration of personal and organization data, including history and templates. Apache-2.0 and MIT are candidates; the license remains undecided.
7. **Reuse without surprising changes.** New occurrences can start from a common baseline. Editing one occurrence must not silently change its siblings, and template edits must not silently change existing events.
8. **Build in small slices.** Begin with staffing/signup. Preserve room for committee planning without building a general project-management suite or speculative infrastructure.

## 3. Design goals and evidence of success

| Goal | Evidence to seek in early evaluation |
| --- | --- |
| Simple volunteer participation | A person can open a shared event on mobile or desktop, understand the available shifts, and complete an authorized signup without organizer assistance. |
| Useful organizer control | An organizer can create staffing needs, inspect coverage, manage assignments, and extend a series on both mobile and desktop. |
| Reliable staffing records | Concurrent requests or approvals cannot overfill a shift; pending approval requests do not reserve places; retries do not duplicate registrations; overlap rules are enforced consistently. |
| Contact privacy and consent | Contacts are private by default. If phone sharing is adopted, only authorized recipients within the volunteer's consent scope can access the number; notification permission does not implicitly permit disclosure. |
| Safe event preparation | Newly created series occurrences remain hidden and reject signups until explicitly enabled through authorized controls. |
| Practical reuse | An occurrence can vary its shifts and capacity while retaining its relationship to the series and original baseline. |
| Independent hosting | Another operator can deploy the application with documented configuration and replace external providers through defined interfaces. |
| Meaningful portability | A representative organization and personal history can be exported and imported into another deployment without losing relationships or exposing protected contacts. |

These are qualitative acceptance goals, not yet a complete test plan or quantified service-level targets.

## 4. Core concepts

| Concept | Meaning |
| --- | --- |
| Organization | A context for membership, invitations, event management, and shared access. An organization is distinct from the deployment hosting it. |
| Event template | A reusable blueprint for event details and staffing structure; it is not a dated signup opportunity. |
| Event series | A grouping of related individual events that can be extended with additional occurrences. Automatic recurrence generation is not yet required. |
| Event / occurrence | One dated event with its own settings, jobs, shifts, registrations, and visibility/signup controls. Events need not be assumed to require a series or template; exact creation flows remain open. |
| Job | A staffing role with a name, description, and eligibility information. |
| Shift | A job at a date and start/end time, with a staffing capacity. One job may have many shifts. |
| Volunteer profile and participant | The person managing their details and the person actually participating may differ, such as a parent registering children. |
| Registration | A participant's shift signup and its status. For approval-required shifts, a pending request is not a confirmed place and consumes no capacity. |
| Waitlist entry | A request for a place when capacity is unavailable. Promotion rules remain open. |
| Contact information | Protected volunteer details, with a scoped phone-sharing exception under discussion. Collection, verification, organizer disclosure, and notification delivery are separate concerns. |

A series starts from an initial template or first event. Each occurrence receives its own starting structure and can then be adjusted independently, including the number of shifts and staffing capacity. Newly added occurrences default to **hidden with signup disabled**.

Visibility, authorization, signup availability, and optional credit requirements are separate concepts. A private event does not inherently require payment. Whether hidden events are inaccessible or merely unlisted, and how links authorize private access, remain design decisions.

For example, a group could establish a recurring food-distribution series from a first event. A later occurrence could need three shifts instead of two. That adjustment would affect the later occurrence alone. Whether future additions use the original baseline or an explicitly updated baseline must be settled before implementation.

## 5. Requested feature support

The following captures product direction. Delivery boundaries are still being negotiated.

| Area | Requested support | Decisions still needed |
| --- | --- | --- |
| Event creation and sharing | Public/private events; share links/QR codes and organization-context sharing, independent of payment. | Meaning of private access and sharing permissions. |
| Optional event credits | Internal deployments without payments; optional credits for hosts that charge; host-defined rates and bring-your-own payment processors. | Credit grants/purchases, ownership, consumption point, occurrence versus series rules, refunds, and processor interface. |
| Organization access | Membership, invitations, and organization-context event creation. | Invitation authority, role scope, event ownership, and personal versus organization ownership. |
| Staffing | Jobs, shifts, capacities, signup/cancellation, organizer assignment/removal, overlap prevention, waitlists, and approval-required shifts. Pending requests do not reserve places; pending counts are visible. | Qualification review, reviewer permissions, capacity at approval, waitlist interaction, conflict overrides, and changes after signup. |
| Volunteer access | Public-link participation without a required account, optional sign-in, and registering others. Proposed phone sharing can be voluntary or required by an event, with optional remembered organizer permission. | Verification, secure return access, group capacity, consent scope/revocation, recipient changes, and guardian/dependent contact handling. |
| Templates and series | Create related occurrences from a template or first event; add later occurrences and customize each independently. | Baseline/version updates, date/time adjustment, and series-wide controls. |
| Communication | Tentative initial direction: app-only notifications, no built-in direct texting, and abuse controls; notification eligibility remains deployment policy. | App/push approach, desktop-only users, delivery limits, verification, recovery, invitations, and people without notification permission. |
| Calendars | Apple, Microsoft, and Google calendar integration, including ICS/iCalendar compatibility. | Download, subscription, add-to-calendar links, or direct provider integration; event/shift/series scope; updates and cancellation behavior. |
| Portability | Import/export personal and organization data between deployments, including history and templates. | Format/versioning, identity matching, conflict resolution, authority to migrate, and protected contact transfer. |
| Operational outputs | Retain the need for rosters, coverage reports, check-in sheets, hours, printable schedules, and CSV outputs. | Which outputs are essential first. No unauthorized contact disclosure; whether phone-sharing consent permits exports remains open. |

Mobile notification options need dedicated research later. Slack, Discord, and BAND integrations for owners are exploratory possibilities, not release commitments. Calendar integration does not imply two-way synchronization or an in-app calendar interface.

### Stakeholder feedback: contact sharing and approval

**Phone-sharing proposal.** An event could offer phone disclosure to authorized managers or require it as a clearly stated condition of signup. A volunteer could separately choose to remember permission for the same organizer, rather than being prompted repeatedly. Declining required disclosure would mean not signing up for that event; storing a phone number must not silently authorize sharing it. Define whether the organizer is an individual or organization, who can see the number, what happens when managers change, withdrawal of permission, exports, and dependent/guardian details. The earlier blanket no-contact-sharing rule is the baseline being reconsidered, not an unconditional restriction alongside an already approved exception.

**Notification preference.** The user leans toward mobile-app notifications only initially and no direct texting from the product, intending to reduce spam abuse opportunities. This remains a design preference rather than a settled delivery architecture. It does not remove the need for abuse controls or resolve verification, account-free signup, or desktop-only access to updates. Sharing a number does not authorize RallyBoro to send SMS.

**Approval-required shifts.** A manager can flag a shift for qualification review, such as age or training. Applicants see a pending-request count and can apply without a promise of assignment. Pending requests do not hold places or block other applicants; only approved assignments consume capacity. Approval must enforce remaining capacity and applicable conflict rules atomically. Review-pending requests are distinct from capacity waitlists. Decide what happens if the shift fills before review, whether new applications remain open once fully assigned, and who can review qualifications. Automatic credential checks and first-come approval are not implied.

### Core product versus hosting policy

The open-source product is deployable independently of any particular commercial offering. Most internal-use deployments are expected to use no payment system. They must be able to operate public/private events without configuring a processor or maintaining a credit balance.

Event credits are an **optional feature**. A host that enables them can define how credits are allocated or purchased, which event actions require them, and the rates it charges. The core staffing domain should deal with credit eligibility through a defined boundary rather than embedding prices or payment-provider calls in signup and event logic. Exact grant and consumption rules remain open.

A payment integration is separately optional: enabling credit accounting must not by itself require checkout. Hosts can bring their own payment processors through an application-owned interface; no processor is selected. Processor-specific purchases and settlement should be translated into credit operations, with authenticated callbacks and duplicate protection. These are proposed architecture responsibilities, not a chosen implementation.

The earlier free-public/no-notifications and $1-private-event concept is retained as a possible policy for one hosted offering. Other hosts can choose different rates or no charges. Visibility, messaging eligibility, credit accounting, and purchase price must not be treated as the same setting. Contact privacy and abuse protections apply to all deployments, including ones without payments.

## 6. Proposed initial delivery sequence

**Proposal for discussion, not an approved MVP checklist.** The smallest useful path is:

1. **Define access and privacy rules.** Resolve public/private event access, account-free management, organization permissions, notification eligibility, and the minimum verified-contact behavior.
2. **Deliver one staffing workflow.** Organizer creates an event and jobs/shifts; volunteer opens the event, signs up, reviews their registration, and cancels; organizer sees coverage and manages assignments. Validate usability on mobile and desktop, capacity, authorization, and contact protection through the same API used by clients.
3. **Add organizational reuse.** Support invitations and organization sharing, templates/series, adding hidden occurrences, and independent adjustments. Decide whether these are part of the first public release or the next slice.
4. **Complete the release boundary.** Choose initial waitlist/dependent support, calendar output, migration capability, and messaging support. Optional credits and payment adapters can be a separate delivery slice rather than a prerequisite for internal deployments. Requested features excluded from the first release must remain explicit follow-up work.

An internal deployment must not depend on payment setup to validate the staffing workflow. A host that chooses to charge must define its credit and purchase policies before enabling them. Messaging requires privacy, verification, cost, and abuse controls regardless of payment status. Any host that disables public-event notifications must settle secure return access without assuming notification exceptions.

Committee tasks, comments, planning sections, and reviewed template improvements remain later planning work. Native apps, complex offline synchronization, advanced scheduling, and broad third-party integrations are not initial commitments.

## 7. Broad architecture

**Prototype direction: Java and Spring Boot, self-hosted on the maintainer's infrastructure with access through Tailscale.** This reflects the maintainer's strong familiarity with Java and Spring Boot. The build foundation targets Java 25 with Spring Boot 4.2.0 dependency management and Maven 3.10.1 through a checked-in Maven Wrapper. PostgreSQL, JDBC/Flyway, a responsive Thymeleaf interface, and Docker Compose application packaging are selected for the foundation. See the [foundation decisions](decisions/001-foundation.md) and [running guide](running.md). PostgreSQL deployment and credentials are independently managed infrastructure outside project commits. Tailscale specifies network access rather than the application runtime; public exposure and the host setup remain to be defined. Managed-cloud hosting and scale-to-zero are not prerequisites for learning from the prototype. Another implementation language can be considered afterward if warranted; a rewrite is not required or planned by this decision.

The following logical boundaries are proposed within one maintainable application. They do not require separate services.

| Boundary | Responsibility |
| --- | --- |
| Mobile and desktop interface | Event discovery/access, signup, personal registrations, and organizer management, with equal importance given to mobile and desktop usability. A responsive web experience is the current product direction; installation and notification details need validation. |
| Application API | A documented contract for the interface, agents, and integrations; input validation, consistent errors, authorization, and safe retry behavior. |
| Identity and authorization | Separate authentication from permissions; scope actions to the relevant person, event, or organization. Use established identity components rather than custom authentication protocols. |
| Event and staffing domain | Organizations, templates, series, occurrences, jobs, shifts, registrations, waitlists, visibility, and signup availability. Enforce capacity and business invariants at the server. |
| Deployment policy and optional credits | Apply host-configured eligibility and optional event-credit rules without hard-coded prices or required payment infrastructure. Keep purchase/payment-provider details outside the staffing domain. |
| Contact protection and notifications | Encrypt contacts, track verification and scoped disclosure consent if adopted, authorize manager access, and apply notification preferences and abuse controls. App-only delivery is the tentative initial direction. |
| Persistence and background work | Durable domain records, migrations, historical attribution, and reliable deferred work where needed. Prefer a model that preserves relationships and supports atomic capacity changes; storage technology remains open. |
| External adapters | Replaceable interfaces for identity, app notifications, calendar outputs, optional payment processors, and later integrations. Built-in SMS is not part of the preferred initial scope; verification channels remain open. Authenticate applicable provider callbacks. |
| Portability and operations | Versioned import/export, backups and restore, deployment configuration, protected operational logging, and migration verification. |

The principal request flow is: a client calls the API; the application authorizes the action and enforces domain rules; durable storage records the result; eligible follow-up work goes through controlled adapters. A client never receives privileged database access or decides authorization for itself.

A notification request refers to a volunteer or registration. Before delivery, the application checks sender scope, event eligibility, volunteer preferences, and abuse policy. Any proposed organizer access to a phone number needs its own scoped disclosure permission; it is not a side effect of notification delivery. Provider access to delivery identifiers is a separate trust relationship from organizer contact access.

## 8. Data protection and deployment portability

Organization boundaries must be enforced for people and agents. Being authenticated must not automatically grant organization access. Public pages should expose event details and appropriate capacity information; participant-name visibility remains open, and minor names must never be public.

At-rest encryption requires an explicit key-management design. Application-level protection, infrastructure encryption, logging, backups, operational access, and recovery must be evaluated together. The privacy promise is not a claim of end-to-end encryption. In independently operated clouds, the capabilities of infrastructure administrators require an explicit trust model; hiding contacts from event/organization roles alone does not establish protection against a host administrator.

Migration must preserve historical records and relationships, including template revisions where present. Personal-data export and organization migration require distinct permissions. A regular organizer download must not reveal contacts outside the applicable consent scope. Whether phone-sharing consent permits export remains open. Decide how a transfer moves protected records and re-encrypts them without exporting sessions or deployment secrets or automatically widening remembered permissions.

Define contact retention/deletion, volunteer consent for migration, historical attribution after account removal, archive compatibility, and collision handling. Migration of data must not accidentally reopen signups, send messages, or create duplicate commitments; exact import activation rules remain to be specified.

## 9. Decisions for the next design round

1. **Release boundary:** Which requested features are necessary for the first useful release, and which can follow?
2. **Access:** What makes an event public, private, hidden, visible, open, or closed? Who controls each transition?
3. **Notification policy and identity:** Can app-only notifications meet initial needs, including desktop-only users? How do verification, recovery, invitations, and returning to account-free signups work without built-in SMS or assumed email delivery?
4. **Optional credits and economics:** Who holds credits, when are they consumed, and do they apply per occurrence or series? How are grants, purchases, refunds, and migration handled? What processor interface and delivery limits support host-defined rates without burdening deployments that do not charge?
5. **Series behavior:** How are baselines versioned or updated, new dates generated, and shared changes proposed without surprising existing occurrences?
6. **Staffing edge cases:** Who approves restricted shifts and checks qualifications? What happens when pending requests outnumber capacity, a shift fills before review, or pending requests overlap? How do waitlists, group signup, and post-signup changes interact?
7. **Contact consent, trust, and portability:** Should phone disclosure be optional or event-required? Who is the remembered organizer, how is permission withdrawn, and does it cover exports or migration? Who handles keys and protected data, including guardian details and independent host access?
8. **Calendars and mobile notifications:** What is the first supported integration method, and what privacy and delivery guarantees are practical?
9. **Remaining technology and license choices:** With Java/Spring Boot, the Maven build foundation, and near-term self-hosting through Tailscale as the prototype direction, select the database, frontend, host/runtime setup, application deployment tooling, providers, and a permissive license after evaluating these requirements.

## 10. Relationship to project notes

This brief is the shareable overview. The detailed working material is retained in [Product direction](product-direction.md), [Volunteer staffing and signup](volunteer-signup.md), and [Deferred committee planning](event-planning.md). Earlier research into existing products is inspiration only. Technology proposals predating the 2026-11-25 Java/Spring Boot prototype direction are historical candidates, not current selections.
