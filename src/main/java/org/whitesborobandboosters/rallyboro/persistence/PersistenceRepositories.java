package org.whitesborobandboosters.rallyboro.persistence;

import org.springframework.stereotype.Component;

/** Typed repositories shared by the existing small services; contains no persistence policy. */
@Component
public record PersistenceRepositories(
    UserRepository users,
    OrganizationRepository organizations,
    MembershipRepository memberships,
    EventRepository events,
    JobRepository jobs,
    ShiftRepository shifts,
    ParticipantRepository participants,
    RegistrationRepository registrations,
    MutationReceiptRepository receipts,
    AgentTokenRepository tokens,
    CredentialRepository credentials,
    ExternalIdentityRepository identities,
    EventManagerRepository managers,
    InvitationRepository invitations,
    AccessAuditRepository audit,
    PilotImportRepository imports,
    ProbeRepository probes) {}
