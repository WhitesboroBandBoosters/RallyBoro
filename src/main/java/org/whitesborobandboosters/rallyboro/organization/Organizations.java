package org.whitesborobandboosters.rallyboro.organization;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.AccessAuditEntity;
import org.whitesborobandboosters.rallyboro.persistence.EventManagerEntity;
import org.whitesborobandboosters.rallyboro.persistence.EventManagerId;
import org.whitesborobandboosters.rallyboro.persistence.InvitationEntity;
import org.whitesborobandboosters.rallyboro.persistence.MembershipEntity;
import org.whitesborobandboosters.rallyboro.persistence.MembershipId;
import org.whitesborobandboosters.rallyboro.persistence.OffsetPage;
import org.whitesborobandboosters.rallyboro.persistence.OrganizationEntity;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.security.Accounts;
import org.whitesborobandboosters.rallyboro.security.SecurityConfig;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.Member;

@Service
@Transactional(readOnly = true)
public class Organizations {
  private final PersistenceRepositories data;
  private final Access access;

  public Organizations(PersistenceRepositories data, Access access) {
    this.data = data;
    this.access = access;
  }

  public record OrganizationData(UUID id, String name) {}

  public record Invitation(
      UUID id,
      UUID organizationId,
      String organizationName,
      boolean canManage,
      boolean canOrganize,
      UUID eventId,
      String expiresAt,
      String status) {}

  public record InvitationGrant(String token, Invitation invitation) {}

  public record AuditEntry(
      UUID id, String actorName, String action, UUID targetId, String createdAt) {}

  private static void require(boolean valid, String code, String message) {
    if (!valid) throw new DomainException(code, message);
  }

  private static DomainException unavailable() {
    return new DomainException("NOT_FOUND", "This organization is unavailable.");
  }

  private static OffsetPage page(int first, int offset, Sort sort) {
    require(
        first >= 1 && first <= 100 && offset >= 0 && offset <= 10000,
        "INVALID_INPUT",
        "Use a page size of 1–100 and offset of 0–10000.");
    return new OffsetPage(first, offset, sort);
  }

  private OrganizationEntity organization(UUID id, boolean lock) {
    return (lock ? data.organizations().lockById(id) : data.organizations().findById(id))
        .orElseThrow(Organizations::unavailable);
  }

  private void manager(Actor actor, UUID org) {
    Access.requireSession(actor);
    if (!access.manageOrganization(actor, org)) Access.denied();
  }

  private void audit(Actor actor, UUID org, String action, UUID target) {
    var entry = new AccessAuditEntity(UUID.randomUUID());
    entry.setOrganizationId(org);
    entry.setActorId(actor.id());
    entry.setAction(action);
    entry.setTargetId(target);
    data.audit().persist(entry);
  }

  @Transactional
  public OrganizationData create(Actor actor, String name) {
    Access.requireSession(actor);
    require(
        name != null && !name.isBlank() && name.length() <= 120,
        "INVALID_INPUT",
        "Organization name must be 1–120 characters.");
    data.users().lockById(actor.id()).orElseThrow(Organizations::unavailable);
    require(
        data.memberships().countByIdUserIdAndCanManageTrue(actor.id()) < 20,
        "LIMIT_EXCEEDED",
        "You can manage up to 20 organizations in this release.");
    var organization = new OrganizationEntity(UUID.randomUUID());
    organization.setName(name.strip());
    data.organizations().persist(organization);
    var membership = new MembershipEntity(new MembershipId(actor.id(), organization.getId()));
    membership.setRole("ORGANIZER");
    membership.setCanManage(true);
    membership.setCanOrganize(true);
    data.memberships().persist(membership);
    audit(actor, organization.getId(), "ORGANIZATION_CREATED", organization.getId());
    return new OrganizationData(organization.getId(), organization.getName());
  }

  @Transactional
  public InvitationGrant invite(
      Actor actor, UUID org, boolean manage, boolean organize, UUID event) {
    var organization = organization(org, true);
    manager(actor, org);
    if (event != null) {
      require(
          !manage && !organize,
          "INVALID_INPUT",
          "Choose an organization permission or one event, not both.");
      require(
          data.events().existsByIdAndOrganizationId(event, org),
          "NOT_FOUND",
          "This event is unavailable.");
    }
    require(
        data.invitations()
                .countByOrganizationIdAndRevokedFalseAndAcceptedByIsNullAndExpiresAtAfter(
                    org, Instant.now())
            < 100,
        "LIMIT_EXCEEDED",
        "Revoke an invitation before creating another.");
    String token = Accounts.secret();
    var invitation = new InvitationEntity(UUID.randomUUID());
    invitation.setOrganizationId(org);
    invitation.setCreatedBy(actor.id());
    invitation.setTokenHash(SecurityConfig.digest(token));
    invitation.setCanManage(manage);
    invitation.setCanOrganize(organize);
    invitation.setEventId(event);
    invitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    data.invitations().persist(invitation);
    audit(actor, org, "INVITATION_CREATED", invitation.getId());
    return new InvitationGrant(token, invitationData(invitation, organization));
  }

  private Invitation invitationData(InvitationEntity invitation, OrganizationEntity org) {
    boolean issuer =
        data.memberships()
            .existsByIdUserIdAndIdOrganizationIdAndCanManageTrue(
                invitation.getCreatedBy(), invitation.getOrganizationId());
    String status =
        invitation.getAcceptedBy() != null
            ? "ACCEPTED"
            : invitation.isRevoked()
                ? "REVOKED"
                : !invitation.getExpiresAt().isAfter(Instant.now())
                    ? "EXPIRED"
                    : !issuer ? "ISSUER_REMOVED" : "AVAILABLE";
    return new Invitation(
        invitation.getId(),
        org.getId(),
        org.getName(),
        invitation.isCanManage(),
        invitation.isCanOrganize(),
        invitation.getEventId(),
        invitation.getExpiresAt().toString(),
        status);
  }

  private InvitationEntity tokenRow(String token) {
    require(
        token != null && token.matches("[A-Za-z0-9_-]{43}"),
        "NOT_FOUND",
        "This invitation is unavailable.");
    return data.invitations()
        .findByTokenHash(SecurityConfig.digest(token))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "This invitation is unavailable."));
  }

  private Invitation usable(Actor actor, InvitationEntity invitation, OrganizationEntity org) {
    var result = invitationData(invitation, org);
    boolean own =
        actor.id().equals(invitation.getAcceptedBy()) && access.member(actor, org.getId());
    require(
        result.status().equals("AVAILABLE") || (result.status().equals("ACCEPTED") && own),
        "NOT_FOUND",
        "This invitation is unavailable.");
    return result;
  }

  public Invitation preview(Actor actor, String token) {
    Access.requireSession(actor);
    var invitation = tokenRow(token);
    return usable(actor, invitation, invitation.getOrganization());
  }

  @Transactional
  public OrganizationData accept(Actor actor, String token) {
    Access.requireSession(actor);
    var initial = tokenRow(token);
    var org = organization(initial.getOrganizationId(), true);
    var invitation =
        data.invitations().lockById(initial.getId()).orElseThrow(Organizations::unavailable);
    usable(actor, invitation, org);
    if (invitation.getAcceptedBy() == null) {
      var key = new MembershipId(actor.id(), org.getId());
      var existing = data.memberships().findById(key);
      var membership = existing.orElseGet(() -> new MembershipEntity(key));
      membership.setCanManage(membership.isCanManage() || invitation.isCanManage());
      membership.setCanOrganize(membership.isCanOrganize() || invitation.isCanOrganize());
      membership.setRole(
          membership.isCanManage() || membership.isCanOrganize() ? "ORGANIZER" : "VOLUNTEER");
      if (existing.isEmpty()) data.memberships().persist(membership);
      if (invitation.getEventId() != null) {
        var grant = new EventManagerId(invitation.getEventId(), actor.id());
        if (!data.managers().existsById(grant))
          data.managers().persist(new EventManagerEntity(grant));
      }
      invitation.setAcceptedBy(actor.id());
      audit(actor, org.getId(), "INVITATION_ACCEPTED", invitation.getId());
    }
    return new OrganizationData(org.getId(), org.getName());
  }

  public List<Invitation> invitations(Actor actor, UUID org, int first, int offset) {
    var page = page(first, offset, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    manager(actor, org);
    return data.invitations().findByOrganizationId(org, page).stream()
        .map(i -> invitationData(i, i.getOrganization()))
        .toList();
  }

  @Transactional
  public boolean revoke(Actor actor, UUID org, UUID id) {
    organization(org, true);
    manager(actor, org);
    data.invitations()
        .findById(id)
        .filter(
            i -> i.getOrganizationId().equals(org) && !i.isRevoked() && i.getAcceptedBy() == null)
        .ifPresent(i -> {
          i.setRevoked(true);
          audit(actor, org, "INVITATION_REVOKED", id);
        });
    return true;
  }

  @Transactional
  public boolean membership(
      Actor actor, UUID org, UUID user, boolean manage, boolean organize, boolean remove) {
    organization(org, true);
    manager(actor, org);
    var key = new MembershipId(user, org);
    var member =
        data.memberships()
            .findById(key)
            .orElseThrow(() -> new DomainException("NOT_FOUND", "This membership is unavailable."));
    if (member.isCanManage() && (remove || !manage)) {
      require(
          data.memberships().countByIdOrganizationIdAndCanManageTrue(org) > 1,
          "CONFLICT",
          "Keep at least one organization manager.");
      data.invitations()
          .findByOrganizationIdAndCreatedByAndAcceptedByIsNull(org, user)
          .forEach(i -> i.setRevoked(true));
    }
    if (remove) {
      data.managers().deleteAll(data.managers().findByIdUserIdAndEventOrganizationId(user, org));
      data.memberships().delete(member);
      data.tokens().findByUserIdAndOrganizationId(user, org).forEach(t -> t.setRevoked(true));
    } else {
      member.setCanManage(manage);
      member.setCanOrganize(organize);
      member.setRole(manage || organize ? "ORGANIZER" : "VOLUNTEER");
    }
    audit(actor, org, remove ? "MEMBER_REMOVED" : "PERMISSIONS_CHANGED", user);
    return true;
  }

  private UUID eventOrganization(UUID event) {
    return data.events()
        .findById(event)
        .orElseThrow(() -> new DomainException("NOT_FOUND", "This event is unavailable."))
        .getOrganizationId();
  }

  @Transactional
  public boolean eventManager(Actor actor, UUID event, UUID user, boolean enabled) {
    UUID org = eventOrganization(event);
    organization(org, true);
    manager(actor, org);
    require(
        access.member(new Actor(user, null, false, true), org),
        "NOT_FOUND",
        "Select an organization member.");
    var key = new EventManagerId(event, user);
    if (enabled) {
      if (!data.managers().existsById(key)) data.managers().persist(new EventManagerEntity(key));
    } else data.managers().deleteById(key);
    audit(actor, org, enabled ? "EVENT_MANAGER_GRANTED" : "EVENT_MANAGER_REMOVED", user);
    return true;
  }

  public List<Member> eventManagers(Actor actor, UUID event, int first, int offset) {
    var page = page(first, offset, Sort.by("user.displayName", "id.userId"));
    UUID org = eventOrganization(event);
    manager(actor, org);
    return data.managers().findByIdEventId(event, page).stream()
        .map(g -> {
          var member =
              data.memberships()
                  .findById(new MembershipId(g.getId().getUserId(), org))
                  .orElseThrow(Organizations::unavailable);
          return new Member(
              g.getId().getUserId(),
              g.getUser().getDisplayName(),
              member.getRole(),
              member.isCanManage(),
              member.isCanManage() || member.isCanOrganize());
        })
        .toList();
  }

  public List<AuditEntry> audit(Actor actor, UUID org, int first, int offset) {
    var page = page(first, offset, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
    manager(actor, org);
    return data.audit().findByOrganizationId(org, page).stream()
        .map(e ->
            new AuditEntry(
                e.getId(),
                e.getActor().getDisplayName(),
                e.getAction(),
                e.getTargetId(),
                e.getCreatedAt().toString()))
        .toList();
  }
}
