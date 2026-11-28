package org.whitesborobandboosters.rallyboro.security;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.EventManagerId;
import org.whitesborobandboosters.rallyboro.persistence.ExternalIdentityId;
import org.whitesborobandboosters.rallyboro.persistence.MembershipId;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;

@Component
@Transactional(readOnly = true)
public class Access {
  private final PersistenceRepositories data;

  public Access(PersistenceRepositories data) {
    this.data = data;
  }

  public record Actor(UUID id, UUID organizationId, boolean agent, boolean writeEnabled) {}

  public static UUID identity(String key) {
    return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
  }

  public Actor actor(Principal principal) {
    if (principal == null || principal instanceof AnonymousAuthenticationToken)
      throw new DomainException("UNAUTHENTICATED", "Please sign in.");
    if (principal instanceof UsernamePasswordAuthenticationToken auth
        && auth.getPrincipal() instanceof Actor actor) return actor;
    if (principal instanceof Authentication auth
        && auth.getPrincipal() instanceof AccountUser user) {
      var stored = data.users().findById(user.id());
      if (stored.isEmpty() || stored.get().getAuthVersion() != user.version())
        throw new DomainException("UNAUTHENTICATED", "Please sign in again.");
      return new Actor(user.id(), null, false, true);
    }
    if (principal instanceof Authentication auth && auth.getPrincipal() instanceof OidcUser oidc) {
      var identity =
          data.identities()
              .findById(new ExternalIdentityId(oidc.getIssuer().toString(), oidc.getSubject()));
      if (identity.isEmpty())
        throw new DomainException(
            "FORBIDDEN", "This sign-in identity has not been linked to an account.");
      return new Actor(identity.get().getUserId(), null, false, true);
    }
    throw new DomainException("UNAUTHENTICATED", "Please sign in.");
  }

  public boolean member(Actor actor, UUID org) {
    return (actor.organizationId() == null || actor.organizationId().equals(org))
        && data.memberships().existsById(new MembershipId(actor.id(), org));
  }

  public boolean organizer(Actor actor, UUID org) {
    if (!member(actor, org)) return false;
    var membership = data.memberships().findById(new MembershipId(actor.id(), org)).orElseThrow();
    return membership.isCanManage() || membership.isCanOrganize();
  }

  public boolean manageOrganization(Actor actor, UUID org) {
    return member(actor, org)
        && data.memberships().existsByIdUserIdAndIdOrganizationIdAndCanManageTrue(actor.id(), org);
  }

  public boolean manageEvent(Actor actor, UUID event, UUID org) {
    return member(actor, org)
        && (organizer(actor, org)
            || data.managers().existsById(new EventManagerId(event, actor.id())));
  }

  public void requireEvent(Actor actor, UUID event, UUID org) {
    if (!manageEvent(actor, event, org)) denied();
  }

  public boolean memberList(Actor actor, UUID org) {
    return member(actor, org)
        && (organizer(actor, org)
            || data.managers().existsByIdUserIdAndEventOrganizationId(actor.id(), org));
  }

  public void requireOrganizer(Actor actor, UUID org) {
    if (!organizer(actor, org)) denied();
  }

  public void write(Actor actor) {
    if (!actor.writeEnabled()) denied();
  }

  public void session(Actor actor) {
    requireSession(actor);
  }

  public static void requireSession(Actor actor) {
    if (actor.agent()) denied();
  }

  public static void denied() {
    throw new DomainException("FORBIDDEN", "You do not have access to this action.");
  }
}
