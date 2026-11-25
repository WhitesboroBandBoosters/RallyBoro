---
title: "Phase 7 \u2014 Independent deployment and data portability"
---

# Phase 7 — Independent deployment and data portability

## Outcome

Another operator can install and maintain RallyBoro, and authorized people can move personal and organization data to a separate deployment with relationships and privacy intact.

## Scope and sequence

1. Complete reproducible installation, configuration, initial administrator setup, database migration, upgrades, backup/restore, and operational diagnostics. Document the self-hosted access model and any separately chosen public exposure. Select the open-source license before claiming licensed redistribution readiness.
2. Define a versioned migration format for personal and organization data: historical events, registrations, participants, organizations, membership attribution, templates, series, and consent records where authorized.
3. Implement separate personal-export and organization-migration permissions. Resolve how protected contacts transfer without becoming visible to an unauthorized organizer; do not use CSV reports or raw backups as a substitute.
4. Implement import validation and preview, identity/identifier mapping, duplicate/conflict handling, atomicity or recoverable partial failure, and destination re-encryption. Exclude sessions and deployment/provider secrets. Require deliberate reconnection of integrations.
5. Validate transfer using two independent deployments and representative history, dependents, approvals, waitlists, and template revisions for features shipped so far. Define compatible format versions as later features are added.

## Decisions before implementation

Resolve migration consent, historical attribution for people not moving, contact key handling, retention/deletion, collision rules, and treatment of remembered organizer permissions. Phone-sharing consent does not automatically authorize export or carry across deployments. Define whether imported events remain inactive until reviewed.

## Completion evidence

- A new operator follows the documented installation and restores a backup successfully.
- An upgrade applies migrations while preserving records; failed-upgrade recovery is demonstrated.
- Authorized personal and organization transfers preserve relationships and history without widening contact access.
- Import retries do not duplicate commitments, reopen signups, or send notifications unexpectedly.
- Unauthorized exports and malformed/incompatible imports fail safely with actionable errors.

## Dependency

Backup and configuration work begins in phase 1, with recovery checks repeated as the schema evolves. Full portability follows the implemented domain from phases 3–6; calendar/provider additions need not block migration of core records. Applicable operational checks are required before public exposure, even if full migration ships later.
