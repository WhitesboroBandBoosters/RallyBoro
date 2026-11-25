---
title: "Event Planning Product Scope"
---

# Event Planning Product Scope

## Status: deferred after the 2026-11-07 direction change

EventManager now prioritizes volunteer staffing/signup for its first MVP. See [product-direction.md](product-direction.md) and [volunteer-signup.md](volunteer-signup.md). The committee-planning design below is retained for later; it does not dictate signup MVP scope, lifecycle, event-creation permissions, or implementation order. Reconcile shared event and organization rules when planning work resumes.

## Roles and permissions

- Administrators create events, appoint administrators, assign or remove event owners, manage templates, archive or reopen events, and delete events.
- Event ownership changes are administrator-only and expected to be uncommon.
- An event may have multiple owners.
- Owners manage their event, committee access, organizational sections, tasks, and lifecycle through Completed.
- Committee members see the entire event and all planning tasks.
- Committee members may create tasks and edit unassigned tasks.
- A task may be assigned to an existing user or an email address. An emailed assignee must authenticate and accept access.
- Planning access always requires authentication and explicit authorization.
- The first administrators are bootstrapped from a comma-separated allowlist of Google email addresses in deployment configuration.

Initial roles:

- Administrator: manage all events, administrators, owners, templates, archival, reopening, and deletion.
- Event owner: manage one event, its committee, sections, all tasks, and forward lifecycle transitions through Completed.
- Committee member: view the whole event; create tasks; edit unassigned tasks; comment on visible tasks.
- Viewer: view the event without editing.

Once assigned, a task's content and status can be edited by its assignee, event owners, and administrators. Other committee members may comment but cannot modify it.

## Events and lifecycle

Each event is single-location and continuous, although it may span multiple days. Store its name, description, location, start date/time, end date/time, timezone, status, owners, source template/revision when applicable, and audit timestamps.

Lifecycle:

1. Planning - committee preparation is underway.
2. Active - planning is considered complete and operational features, including future signup, may be enabled.
3. Completed - the event has ended and becomes read-only.
4. Archived - hidden from normal views but retained.

There is no Draft state. Owners and administrators may move an event forward from Planning to Active to Completed. Moving to Active with unfinished tasks displays a warning but is not blocked. Only administrators may archive, move an event backward, or reopen it.

Only one non-completed event instance may originate from a given template. Creation of another is blocked rather than merely warned. Completed and Archived instances remain available as history.

Owners may edit event name, description, location, start/end date and time, timezone, and planning content. Administrators alone control owners, source-template association, reopening, archival, and deletion.

## Templates and yearly reuse

- An Event Template is a reusable blueprint such as Holly Jolly.
- An Event Instance is a dated occurrence such as Holly Jolly 2026.
- Instantiation copies approved template content into an independent snapshot.
- Template changes never mutate an existing event.
- Templates contain reusable event details, organizational sections, and tasks.
- Assignments and event-specific people are always cleared both when deriving a template and when instantiating an event.
- Templates do not initially contain committee membership, default role slots, or named positions.
- Owners can explicitly suggest a change for future events while editing.
- At event completion, owners can review differences from the source template and propose selected changes.
- Proposals may include additions, edits, and deletions.
- Administrators can approve or reject individual changes, edit proposed values, group related changes, and add explanatory notes.
- Approved changes create a new immutable template revision.
- Record the proposer, approver, decisions, notes, and resulting revision.
- Older revisions can be viewed. A special restore operation is unnecessary initially; an administrator may copy an older revision into a new revision.
- An administrator may derive a template from a completed event after removing assignments, people, comments, task states, and other execution history.
- The approval interface must favor clarity over exposing a raw technical diff.

## Task organization and model

The hierarchy is:

Organization -> Event Template -> Event Instance -> Sections -> Tasks

Sections are simple organizational headings with a name, optional description, and manual ordering. They do not have owners, dates, or status.

A task has:

- Title
- Optional description
- Status: Not Started, In Progress, Completed, or Cancelled
- Zero or one assignee
- Optional reference links
- Comments
- Creator
- Created, updated, and completed timestamps
- Position within its section

A task deliberately has no due date, priority, dependency graph, subtasks, estimated effort, or multiple assignees. All planning work is understood to be due before the event begins. A single assignee preserves accountability and manages any informal collaboration needed.

Completed means the work was performed. Cancelled means it was intentionally deemed unnecessary. The assignee, event owners, and administrators may move a task among any statuses, including reopening it.

## Task editing, organization, and history

- Reference links have a title and URL and remain separate from comments.
- Anyone able to edit a task may add, edit, reorder, or remove its links.
- Comment authors may edit or delete their comments. Edited comments are marked edited; visible version history is unnecessary.
- Event owners and administrators may remove any comment.
- Anyone able to edit a task may move it between sections and reorder it.
- Event owners and administrators may remove any task.
- Committee members may remove only unassigned tasks they created.
- Task removal is recoverable and recorded in activity history.
- File attachments are not part of the initial scope.

Useful views include event overview, tasks by section, My Tasks, Unassigned Tasks, and Completed Tasks. Due-soon, upcoming, and overdue views are unnecessary.

## Notifications

Initial planning notifications are limited to:

- Committee invitation
- Task assignment or reassignment

EventManager is not a replacement for committee communication. Task completion sends no notification; owners use the event overview to see remaining work.

## First planning milestone

Google Login -> Organization -> Event -> Section -> Task -> Assign / Edit / Complete

Then add committee invitations and permissions, reusable templates, template-change proposals and administrator approval, comments and reference links, and only evidence-backed later capabilities.

## Product decisions complete

The event-planning product questions required before architecture design are resolved. Any new behavior should be justified by user feedback rather than inferred from generalized project-management products.
