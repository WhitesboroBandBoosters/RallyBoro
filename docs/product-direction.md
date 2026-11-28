---
title: "Product direction and working thoughts"
---

# Product direction and working thoughts

Named RallyBoro on 2026-11-20, to be hosted in the WhitesboroBandBoosters GitHub organization. EventManager is the earlier working name retained in historical notes.

Recorded 2026-11-07 from the user's stream-of-consciousness notes. These capture intent for further design; they are not a finalized MVP specification or authorization to implement every feature.

Updated 2026-11-08 with mobile priority and event template/series requirements.

Updated 2026-11-09 with calendar integration and data portability requirements.

Clarified 2026-11-09: mobile and desktop experiences have equal importance; mobile is not the primary target at desktop's expense.

Updated 2026-11-19 with stakeholder proposals for consent-based phone disclosure and approval-required shifts, and a tentative preference for app-only notifications rather than direct texting.

Updated 2026-11-25 with the Java/Spring Boot prototype direction and near-term self-hosting through Tailscale.

Updated 2026-11-28 with self-service organization creation, scoped permissions, and proximity-based discovery of local public events.

The [shareable design brief](design-brief.md) consolidates these notes into principles, goals, requested feature support, a proposed delivery sequence, and broad architecture. The earlier 2026-11-09 position that no implementation technology was selected is superseded by the prototype direction below.

## Prototype technology and hosting direction — 2026-11-25

- Use Java and Spring Boot for the prototype, reflecting the maintainer's strong familiarity with both and preference for code they can understand and maintain.
- Run on the maintainer's own infrastructure with access through Tailscale in the near term. Tailscale describes network access; the host machine, operating system, process/container setup, and exposure beyond the tailnet remain to be specified. This does not change the product's eventual public-event requirements.
- Prioritize learning and validating volunteer staffing/signup behavior over optimizing for a managed-cloud platform or scale-to-zero deployment. A straightforward self-hosted application is acceptable.
- The initial build foundation targets Java 25 with Spring Boot 4.2.0 dependency management and Maven 3.10.1 through a checked-in Maven Wrapper. See the [development setup](../README.md#development-setup) for build commands. Initiative 1 now provides a runnable application foundation; staffing follows in initiative 2.
- The foundation uses PostgreSQL with JDBC/Flyway, Spring MVC/Thymeleaf, and a non-root Docker app container through Tailscale Serve. The interactive pilot now uses React/TypeScript and Spring GraphQL in place of the original Thymeleaf shell. Spring Security OIDC is selected for phase-2 authentication integration. See the [foundation decisions](decisions/001-foundation.md); the database deployment and all credentials remain external infrastructure.
- The implementation can be rewritten in another language after prototyping if experience warrants it. A rewrite is an option, not a planned requirement; preserve clear API and data boundaries without speculative abstraction.

## API and frontend clarification — 2026-11-27

GraphQL is required for the user/agent application API. Build an interactive responsive frontend rather than carrying the temporary Thymeleaf shell forward as a form-post product UI. Use strict typing and schema-derived operation types; React/TypeScript with GraphQL Request is selected in [decision 003](decisions/003-staffing-pilot.md). See [decision 002](decisions/002-graphql-and-interactive-ui.md) and initiative-2 issues #17–#18. Initiative 2 implements these capabilities for a controlled synthetic-data pilot.

## Direction change

Focus on **volunteer staffing/signup first**, to reach an MVP before adding committee planning. Preserve the earlier planning design for later. Inscriva and OpenVolunteer can provide inspiration, but neither will be adopted as the actual solution. Their source review remains in [open-source-options.md](open-source-options.md).

## Product thoughts

- **API first and agent friendly.** Design the product so agents and other clients can work through its API. Agent access must respect ordinary authorization, contact privacy, and abuse controls. API authentication, documentation, and automation ergonomics remain to be designed; no specific agent protocol is selected.
- **Mobile and desktop are equally important.** Volunteer signup and organizer workflows must work well on both, with layouts and interactions suited to each. Neither experience is secondary. Research notification options at a later design stage; notification eligibility is deployment policy, and no new notification channels are selected.
- **Local public-event discovery (requested 2026-11-28).** People should be able to search for public events by proximity to a location, rather than needing an organization invitation or an existing event link. Support this through the GraphQL API and responsive UI. Private and hidden events must not appear in discovery results or leak through counts, locations, or other search metadata. Decide location input (for example city/postal code, an entered address, or optional device location), radius/units, date filters, distance ordering, venue coordinates/geocoding, location precision/privacy, and whether public-but-unlisted links are distinct from discoverable public events. Public browsing authentication and cross-deployment discovery remain open; no map/geocoding vendor or federation is selected. Record the feature in phase-3 discovery planning and define its release boundary before implementation.
- **Calendar integration.** Support Apple Calendar, Microsoft calendars, and Google Calendar, including ICS/iCalendar compatibility. Decide later whether this means downloadable files, subscribable feeds, add-to-calendar links, direct provider APIs, or a combination. Two-way synchronization and in-app calendar views are not implied. Define event/shift/series scope, updates and cancellations, timezone handling, and private calendar access before implementation.
- **Templates, series, and occurrences.** Distinguish a reusable event blueprint from a grouping of related dated events and from each individual event. A series can be extended. Each event starts from the initial template or first event, can be adjusted independently (including its shifts), and defaults to hidden with signups disabled. See [volunteer-signup.md](volunteer-signup.md) for the working model and remaining questions.
- **Optional event credits and host-defined pricing.** Separate event costs from the overall open-source product. Most internal-use deployments are expected to operate without payments. Hosts may optionally enable event credits, charge their own rates, and bring their own payment processors. Credits and payment processing are separate optional concerns; neither is required for core event functionality. Credit ownership, grants, purchase/consumption rules, refunds, and migration remain to be designed.
- **Earlier hosted-offering proposal.** Free public events with no notifications and $1 private events were proposed for a particular hosted offering. They are not universal pricing or notification rules for every deployment. Preserve abuse protections while allowing deployment-specific policy. Organizers can share event links/QR codes or share to organizations they manage; visibility/access semantics remain separate from credits and payment.
- **Organizations and scoped permissions (clarified 2026-11-28).** Any signed-in user can create an organization and receives permission to manage it and invite others. Permissions apply to the relevant organization/event rather than a global administrator, organizer, or volunteer user category; one account can have different permissions in different contexts. No organization bootstrap or pre-enrolled administrator is required for this product workflow. Exact grants, delegation/inheritance, and transfer rules remain to be designed. The phase-2 enrollment file and coarse membership roles are pilot implementation details, not the target model.
- **Data portability and self-hosting.** Support import and export so people can move their personal data and organization data between deployments, including deployments hosted in someone else's cloud. Include previous history and templates, preserving relationships among organizations, memberships, event series, events, jobs, shifts, registrations, and template revisions where present. Migration is a product capability, beyond reporting exports or provider-specific database backups. Preserve the contact-privacy promise: an organization migration must not disclose contacts outside authorized scope; whether proposed phone-sharing consent covers exports remains open. Authorized personal-data transfer and protected organization-data transfer need distinct permission boundaries. The archive format, compatibility/versioning, identity mapping, conflict behavior, and encryption/key transfer or re-encryption design remain open.
- **Volunteer contact privacy and proposed phone sharing.** Contacts remain private by default and encrypted at rest. Stakeholders propose allowing a volunteer to share a phone number with authorized event managers, allowing an event to require that disclosure before signup, and offering an explicit remembered permission for the same organizer. This is a scoped exception under discussion to the earlier no-contact-sharing rule, not unrestricted organizer access. Consent scope, revocation, exports, manager changes, minors, and migration remain unresolved. Email disclosure is not proposed. Apply the eventual policy consistently to APIs, agents, screens, integrations, reports, and exports; encryption at rest is not a claim of end-to-end encryption.
- **Short-term notification preference (tentative).** The user now leans toward no built-in direct texting and notifications only through the mobile app initially. Earlier mediated text/email delivery is retained as historical context, not the preferred initial implementation. No native/PWA/push technology is selected. Delivery limitations, desktop-only users, verification/recovery, account-free participation, and abuse monitoring still require design; app-only delivery does not itself settle abuse prevention.
- **Approval-required shifts.** A manager can require review of qualifications such as age/training. Pending requests do not reserve capacity; volunteers can see a pending count and still apply, with no guarantee of assignment. Only approval consumes capacity. Enforce capacity and conflict rules at approval; decide review authority, evidence, pending conflicts, and the relationship to waitlists separately. See [signup details](volunteer-signup.md).
- **Possible owner integrations.** Explore Slack and Discord bots for event owners. BAND support was also raised; findings are below. These are possibilities rather than committed MVP channels. Integrations must preserve volunteer contact privacy and deployment messaging policies.
- **Open source.** Publish the codebase under a permissive license, probably Apache-2.0 or MIT. The final license is not selected; no license file has been added.

## BAND integration research

Checked 2026-11-07 against NAVER BAND's official developer documentation (the group app at band.us).

BAND offers an [Open API](https://developers.band.us/develop/guide/api), including [creating group posts](https://developers.band.us/develop/guide/api/write_post) with an optional `do_push` flag for subscribed group members. Access uses an authorized user's token. This supports an automated group-posting integration, but the inspected documentation does not establish a Discord-style bot identity, individual direct-message delivery, or inbound bot commands. Those capabilities should not be assumed.

New OAuth client registrations require preliminary BAND review before client credentials are issued, under a [notice effective September 15, 2025](https://developers.band.us/develop/policy/notice). An owner-authorized BAND integration is therefore a possible exploration, subject to approval and capability checks.

## Questions to resolve before implementation

- What distinguishes public/private: discoverability, possession of a link, organization membership, invitations, or a combination?
- How should nearby public-event search accept a location, define distance/time filters, obtain venue coordinates, and protect search/venue location privacy? Are all public events discoverable, and is the initial scope one deployment or a cross-deployment directory?
- For hosts that disable public-event notifications, does that policy cover verification, organization invitations, and recovery? How do volunteers securely return to manage registrations?
- How does the tentative app-only notification direction work for verification, recovery, desktop-only users, and people without app notifications? What message and abuse limits apply independently of payment?
- What is the smallest signup slice: jobs, shifts, capacity, signup/cancellation, and which parts of waitlists, dependents, accounts, and organization access?
- Which scoped organization/event permissions cover creating/managing events, inviting others, delegating access, and any future messaging? How do they relate to later planning permissions? Organization creation by any signed-in user is already selected.
- What encryption, retention, contact matching, and abuse-monitoring design supports private-by-default contacts and scoped phone-sharing consent? Who is the remembered organizer, what if managers change, and can shared numbers be exported or migrated?
- Which permissive license should be selected?
- How are series extended and scheduled, and which baseline is used for new occurrences after edits?
- When optional credits are enabled, who holds them, how are they granted/purchased, what consumes them, and how do occurrence/series rules, refunds, payment adapters, and cross-deployment migration work?
- Who may export/import personal data or an organization, and how are private contact records transferred without disclosing them to organizers? How are identities, memberships, historical attribution, template revisions, encryption, and existing destination data reconciled across deployments?

The earlier React/TypeScript PWA and Cloudflare proposal is historical technical context, not a selected stack or current default. Java and Spring Boot are the current prototype direction; evaluate the remaining technology choices against the signup MVP, privacy model, portability, notification needs, and optional payment behavior.
