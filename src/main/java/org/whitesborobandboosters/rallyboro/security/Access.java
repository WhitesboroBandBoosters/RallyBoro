package org.whitesborobandboosters.rallyboro.security;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;

@Component
public class Access {
  private final JdbcTemplate jdbc;

  public Access(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
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
      var versions = jdbc.queryForList("SELECT auth_version FROM app_user WHERE id=?", user.id());
      if (versions.isEmpty() || !versions.getFirst().get("auth_version").equals(user.version()))
        throw new DomainException("UNAUTHENTICATED", "Please sign in again.");
      return new Actor(user.id(), null, false, true);
    }
    if (principal instanceof Authentication auth && auth.getPrincipal() instanceof OidcUser oidc) {
      var identities =
          jdbc.queryForList(
              "SELECT user_id FROM external_identity WHERE issuer=? AND subject=?",
              oidc.getIssuer().toString(),
              oidc.getSubject());
      if (identities.size() != 1)
        throw new DomainException(
            "FORBIDDEN", "This sign-in identity has not been linked to an account.");
      return new Actor((UUID) identities.getFirst().get("user_id"), null, false, true);
    }
    throw new DomainException("UNAUTHENTICATED", "Please sign in.");
  }

  public boolean member(Actor actor, UUID org) {
    return (actor.organizationId() == null || actor.organizationId().equals(org))
        && jdbc.queryForObject(
                "SELECT count(*) FROM membership WHERE user_id=? AND organization_id=?",
                Integer.class,
                actor.id(),
                org)
            == 1;
  }

  public boolean organizer(Actor actor, UUID org) {
    return member(actor, org)
        && jdbc.queryForObject(
                "SELECT count(*) FROM membership WHERE user_id=? AND organization_id=? AND"
                    + " (can_manage OR can_organize)",
                Integer.class,
                actor.id(),
                org)
            == 1;
  }

  public boolean manageOrganization(Actor actor, UUID org) {
    return member(actor, org)
        && jdbc.queryForObject(
            "SELECT can_manage FROM membership WHERE user_id=? AND organization_id=?",
            Boolean.class,
            actor.id(),
            org);
  }

  public boolean manageEvent(Actor actor, UUID event, UUID org) {
    return member(actor, org)
        && (organizer(actor, org)
            || jdbc.queryForObject(
                    "SELECT count(*) FROM event_manager WHERE user_id=? AND event_id=?",
                    Integer.class,
                    actor.id(),
                    event)
                > 0);
  }

  public void requireEvent(Actor actor, UUID event, UUID org) {
    if (!manageEvent(actor, event, org)) denied();
  }

  public boolean memberList(Actor actor, UUID org) {
    return organizer(actor, org)
        || (member(actor, org)
            && jdbc.queryForObject(
                    "SELECT count(*) FROM event_manager g JOIN event e ON e.id=g.event_id WHERE"
                        + " g.user_id=? AND e.organization_id=?",
                    Integer.class,
                    actor.id(),
                    org)
                > 0);
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
