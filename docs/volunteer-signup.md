---
title: "Volunteer Staffing and Signup"
---

# Volunteer Staffing and Signup

## Status and boundary

As of 2026-11-07, volunteer staffing/signup is the first MVP direction. Committee planning will follow later. See [product-direction.md](product-direction.md) for the new pricing, organization, API, privacy, messaging, and open-source thoughts. These newer requirements supersede conflicting assumptions below. MVP boundaries still need to be selected; retained signup decisions are not permission to implement the entire module at once.

Planning and signup share an event instance but remain separate sections. Keep planning tasks distinct from jobs and shifts. Inscriva and OpenVolunteer are inspiration only and will not be adopted as the solution.

## Decisions already made

- Volunteers use a public event link and may register without an account or sign in to preserve contact details.
- A person may register others, including minor children. Planning access remains authenticated and invitation-only.
- Owners can assign and remove volunteers.
- A Job has a name, description, and eligibility information.
- A Shift applies a job at a date, start time, end time, and capacity; one job may have many shifts.
- Event templates or a first event provide the starting structure for events in a series, including jobs and shifts. Planning tasks and shifts remain separate.
- Prevent overlapping confirmed registrations for one person. Eligibility information can also be reviewed by a manager on approval-required shifts; qualification checking is not assumed to be automated. Conflict handling for multiple pending requests remains open.
- Waitlists are required.
- Administrators assign event owners. Owners propose reusable changes; only administrators select and approve template updates.
- Template changes never alter an ongoing event. A completed event may become a template after personal and execution-history data is removed.
- Volunteers may cancel. Cancellation/waitlist notifications must respect deployment notification policy, volunteer preferences, and abuse controls. The earlier notification-free public tier is a possible hosted-offering policy, not a universal product restriction.
- Public pages show high-level event details and shifts.
- Calendar integration is requested for Apple Calendar, Microsoft calendars, and Google Calendar, including ICS/iCalendar support (2026-11-09). The delivery method and initial release scope remain open; do not assume two-way synchronization.
- Future outputs include rosters, unfilled-shift reports, check-in sheets, volunteer hours, printable schedules, CSV exports, and per-event calendar files.
- Earlier discussions assumed simultaneous operational instances of the same type were not expected. The new series requirement permits multiple occurrences to be created in advance; do not apply the deferred planning model's one-non-completed-instance-per-template restriction to block them. Whether open occurrences may overlap remains to be decided.

## Mobile and desktop experience, and notifications

Mobile and desktop experiences are equally important, including volunteer signup and organizer event/shift management. Neither is the primary target at the other's expense. A responsive web experience remains the product direction; the earlier PWA proposal is an implementation candidate, and frontend and notification technologies remain open. The Java/Spring Boot backend prototype direction is recorded in [product-direction.md](product-direction.md). As of 2026-11-19, the preferred short-term direction is notifications through the mobile app and no built-in direct texting, superseding the earlier text/email preference as the initial delivery proposal. This remains tentative and does not select a native app, PWA, or push provider. Resolve verification, account-free signup, returning to manage requests, and access to updates for desktop-only users or people without notification permission. App delivery is intended to reduce abuse opportunities, not eliminate the need for authorization, limits, and abuse controls. Eligibility remains deployment policy, separate from visibility and optional payment.

## Stakeholder proposal: consent-based phone sharing

Recorded 2026-11-19 for design discussion. This proposes revising the earlier absolute prohibition on organizer access to contact details; it is not approval for unrestricted contact disclosure.

- A volunteer could explicitly share their phone number with the event's authorized managers.
- An event manager could make phone-number disclosure a condition of signup. Show that condition and the intended recipients before submission; declining would mean not signing up for that event, rather than silently disclosing a stored number.
- A volunteer could choose to remember permission to share with the same organizer, avoiding repeated prompts for that organizer's future events. Do not infer this permission from one-time disclosure.
- Phone disclosure is separate from sending texts through EventManager, receiving app notifications, or sharing an email address. Keep contacts encrypted at rest and private outside the authorized disclosure scope.
- Define whether an organizer means a person, organization, or manager group; what happens when managers change; how permission is withdrawn; whether phone numbers appear in exports; and how guardian/dependent information is handled. Do not automatically migrate or widen remembered permissions across deployments.

Until this policy is settled, interpret older blanket privacy statements as the original baseline, with this scoped phone-sharing exception under discussion.

## Requested capability: approval-required shifts

An event manager can mark a shift as requiring approval, for example to check age or training qualifications. A signup is then a **pending request**, not a confirmed assignment.

- Pending requests do not consume or reserve shift capacity and do not prevent other volunteers from applying.
- Show volunteers how many requests are awaiting approval, separately from approved assignments and remaining capacity. A visible count does not imply public applicant names or qualification details.
- Clearly tell applicants that their request is awaiting approval and does not guarantee a place.
- Capacity is consumed only when a manager approves an assignment. Approval must check remaining capacity and applicable overlap rules atomically, including simultaneous approvals.
- Pending qualification review and a capacity waitlist are distinct concepts. Decide what happens when capacity fills before a pending request is reviewed, and whether applications may continue after all places are assigned.

Proposed state labels are Pending, Approved, Declined, and Withdrawn; exact names and transitions remain open. Also define reviewer permissions, required evidence, review history, whether qualifications can be reused, pending-request conflicts, and behavior when approval requirements change after people sign up. Do not assume first-come approval, automated qualification verification, or a capacity guarantee for pending applicants.

## Optional event credits and hosting policy

Core event creation and staffing must work without a payment system, as expected for most internal deployments. Event credits are optional. Hosts that choose to enable them can define their own rates and rules and bring their own payment processors. Credit accounting must not inherently require a processor; allocation/purchase mechanisms and consumption rules remain to be designed.

The earlier $1-private/free-public proposal belongs to a potential hosted offering, not the open-source domain model. Private visibility does not inherently require payment, and messaging eligibility is not synonymous with a credit balance. Define credit ownership, event/series consumption rules, refunds, transfer behavior, and processor interfaces separately from core signup requirements.

## Event templates, series, and individual events

Added 2026-11-08. These concepts must remain distinct:

- **Event template:** a reusable blueprint for initial event content and staffing structure. It is not itself a dated signup opportunity.
- **Event series:** a grouping of related individual events. Organizers must be able to add further events to an existing series. Automatic recurrence generation is not yet a requirement.
- **Individual event / occurrence:** one dated event within a series, with its own shifts, capacities, registrations, and settings. Each starts from the initial template or first event. Organizers can adjust it independently, including adding/removing shifts or changing staffing needs.

New events in a series are **hidden by default and do not allow signups**. Visibility and signup availability are separate concerns; creating an occurrence must not implicitly publish it or open registration. Exact publication controls and the meaning of hidden for direct-link access remain to be designed.

Keep the previously established snapshot principle: editing a template must not silently change an existing event. An occurrence's adjustments must not silently alter sibling occurrences. The baseline/version used when adding later occurrences, and any explicit way to promote an occurrence's improvements back to that baseline, remain open. Do not assume edits to the first event retroactively propagate.

Remaining series questions:

- Does the series capture a fixed snapshot of its initial template/first event, or may organizers explicitly update the baseline for future additions?
- How are new event dates and shift times supplied? Is manual addition sufficient initially, and how should timezone/daylight-saving changes be handled?
- Who can add occurrences, make them visible, and open signups? Should any controls apply to multiple occurrences at once?
- Does hidden mean unlisted but link-accessible, or inaccessible to volunteers until publication? How does it relate to private-event access, independent of payment?
- Is signup for each occurrence independent, or should a future volunteer be able to claim several occurrences at once?
- If a deployment enables credits, what event action consumes them and does the rule apply per occurrence or per series?
- May multiple occurrences be visible/open simultaneously, including overlapping dates?

## Architectural seams to preserve

- Stable event-instance identifiers and revisioned templates capable of later holding jobs and relative shifts.
- Separate planning authorization from public-signup authorization and public pages from committee views.
- Distinct Job, Shift, VolunteerProfile, Participant, Registration, pending approval request, and WaitlistEntry concepts; do not overload Task or Membership, or treat approval-pending requests as capacity reservations.
- Provider-neutral notification and email interfaces.
- Core event timezone, start/end dates, and scheduling anchor shared by planning and future signup.
- Portable import/export for personal and organization data across deployments, including history and templates; preserve relationships without relying on one cloud provider's database backup format.

## Import, export, and deployment migration

Requested 2026-11-09. People must be able to move their personal data and organization data from one deployment to another, including self-hosted deployments in another person's cloud. Include historical records and templates, not only currently active events. Preserve event-series relationships, staffing structures, registrations, memberships, template revisions, and historical attribution where present.

This is distinct from CSV reports and infrastructure backups. Migration must respect contact access permissions: export files and import tools must not reveal contacts that an organizer is not authorized to see. Whether the proposed phone-sharing consent permits exports remains unresolved. Personal exports concern the requesting person's authorized data; organization migration requires an authorized transfer mechanism for protected data. The mechanism and permissions are still to be designed.

Open migration questions include archive format/version compatibility, identity and identifier mapping, duplicate/conflict handling, encrypted transfer and destination re-encryption, historical attribution when an account is not migrated, and which deployment-specific credentials or integrations must be reconnected. Migration should not be assumed to carry active sessions or provider secrets to the destination.

## Questions remaining

- Which contact fields and verification are required under the app-only notification proposal? How does event-required phone disclosure interact with optional/remembered consent, account-free signup, dependent participants, and deployments that restrict notifications?
- How are anonymous people matched for duplicate and overlap detection?
- Are dependents reusable participant profiles? How are groups handled when capacity is insufficient?
- Which qualification information is needed for approval-required shifts, who reviews it, and what evidence or review history is retained?
- Can volunteers see names or only capacity counts? Minor names must never be public.
- May owners override conflicts? Are back-to-back shifts non-overlapping?
- Is waitlist promotion automatic or offered with an expiration? Can owners reorder it?
- Which lifecycle states control signup, and who opens or closes it?
- What may change after registrations exist, and which changes notify volunteers?
- Which events notify owners, and what reminder schedule and channels are defaults?
- Are check-in/out, no-shows, and actual hours required?
- Which reports are mandatory first? Reports and exports must not disclose unauthorized contacts; whether proposed phone-sharing permission includes export remains to be decided.
- Which calendar options ship first: downloadable ICS/iCalendar files, subscription feeds, provider add-to-calendar links, or direct Apple/Microsoft/Google integrations? Should calendars include selected shifts, one event, or a series, and how should updates, cancellations, timezone changes, and private feed access work?
- How long are contact details and historical rosters retained?
- May next year's instance be planned while only one instance of a type is publicly active?

## Earlier recommended assumptions (subject to the new direction)

Earlier recommendations were: emailed secure management links for anonymous registrations; dependents under a primary contact; only capacity counts public; audited owner conflict overrides; end-equals-start is non-overlapping; waitlist offers with expiration; one downloadable ICS per volunteer per event; next year's planning allowed while only one instance is publicly active. These remain proposals, not newly accepted requirements. Email links and waitlist offers need reconciliation with each deployment's notification policy; same-template overlap also needs reconciliation with the retained planning rules.
