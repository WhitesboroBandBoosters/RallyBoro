package org.whitesborobandboosters.rallyboro.security;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.AgentToken;
import org.whitesborobandboosters.rallyboro.staffing.Models.TokenGrant;

@Service
public class AgentTokens {
  private final JdbcTemplate db;
  private final Access access;

  public AgentTokens(JdbcTemplate db, Access access) {
    this.db = db;
    this.access = access;
  }

  public List<AgentToken> list(Actor actor) {
    access.session(actor);
    return db.query(
        "SELECT * FROM agent_token WHERE user_id=? ORDER BY expires_at DESC LIMIT 100",
        (r, i) ->
            new AgentToken(
                r.getObject("id", UUID.class),
                r.getString("name"),
                r.getObject("organization_id", UUID.class),
                r.getBoolean("write_enabled"),
                r.getObject("expires_at", OffsetDateTime.class).toInstant().toString(),
                r.getBoolean("revoked")),
        actor.id());
  }

  @Transactional
  public TokenGrant create(Actor actor, UUID org, String name, boolean write) {
    access.session(actor);
    if (!access.member(actor, org)) Access.denied();
    if (name.isBlank() || name.length() > 80)
      throw new DomainException("INVALID_INPUT", "Token name must be 1–80 characters.");
    if (db.queryForObject(
            "SELECT count(*) FROM agent_token WHERE user_id=? AND NOT revoked AND expires_at>now()",
            Integer.class,
            actor.id())
        >= 10)
      throw new DomainException(
          "LIMIT_EXCEEDED", "Revoke an existing token before creating another.");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = "rb_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    UUID id = UUID.randomUUID();
    OffsetDateTime expires = OffsetDateTime.now().plusDays(7);
    db.update(
        "INSERT INTO"
            + " agent_token(id,user_id,organization_id,name,token_hash,write_enabled,expires_at)"
            + " VALUES (?,?,?,?,?,?,?)",
        id,
        actor.id(),
        org,
        name.strip(),
        SecurityConfig.digest(token),
        write,
        expires);
    return new TokenGrant(
        token, new AgentToken(id, name.strip(), org, write, expires.toInstant().toString(), false));
  }

  @Transactional
  public boolean revoke(Actor actor, UUID id) {
    access.session(actor);
    db.update("UPDATE agent_token SET revoked=true WHERE id=? AND user_id=?", id, actor.id());
    return true;
  }
}
