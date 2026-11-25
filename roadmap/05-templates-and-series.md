---
title: "Phase 5 \u2014 Templates and series"
---

# Phase 5 — Templates and series

## Outcome

An organizer can reuse staffing structure, add dates to a series, and adjust each occurrence independently.

## Scope and sequence

1. Introduce reusable event templates and series as distinct concepts from dated events. Create a series from a template or first event with an explicit starting baseline.
2. Add occurrences manually with their own jobs, shifts, capacities, and settings. Each new occurrence defaults to hidden with signups disabled; enforce this in the API as well as the UI.
3. Permit occurrence-specific adjustments without changing siblings. Template edits must not silently change existing events.
4. Support creating a template from an event while excluding volunteers, registrations, private contacts, and execution history. Apply the agreed template-management permissions.

## Decisions before implementation

Define baseline snapshots/versioning, who can promote improvements into a baseline, whether future additions may use an explicitly updated baseline, date/time adjustment across daylight-saving changes, and publication authority. Resolve historical administrator/owner template permissions against the new organization roles. Manual additions are sufficient initially; automatic recurrence and batch signup need separate scope.

## Completion evidence

- Add a later occurrence, change its shift count/capacity, and demonstrate that the initial event and siblings are unchanged.
- New occurrences cannot accept signup until authorized visibility/access and signup conditions are satisfied.
- Template updates affect only their defined future scope; copies contain no participant data.
- Tests cover baseline isolation, permissions, time conversion, and concurrent publication/signup boundaries.

## Dependency

Builds on phase 3 and reuses the phase-2 event model. Preserve phase-4 approval settings if that initiative has shipped. Enables richer calendar output and migration fixtures in phases 6–7.
