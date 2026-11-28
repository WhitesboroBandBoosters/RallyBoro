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
    String key = "local:" + principal.getName();
    if (principal instanceof Authentication auth && auth.getPrincipal() instanceof OidcUser oidc) {
      key = oidc.getIssuer() + "|" + oidc.getSubject();
    }
    UUID id = identity(key);
    if (jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=?", Integer.class, id) != 1)
      throw new DomainException("FORBIDDEN", "This account is not enrolled in the pilot.");
    return new Actor(id, null, false, true);
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
                    + " role='ORGANIZER'",
                Integer.class,
                actor.id(),
                org)
            == 1;
  }

  public void requireOrganizer(Actor actor, UUID org) {
    if (!organizer(actor, org)) denied();
  }

  public void write(Actor actor) {
    if (!actor.writeEnabled()) denied();
  }

  public void session(Actor actor) {
    if (actor.agent()) denied();
  }

  public static void denied() {
    throw new DomainException("FORBIDDEN", "You do not have access to this action.");
  }
}
