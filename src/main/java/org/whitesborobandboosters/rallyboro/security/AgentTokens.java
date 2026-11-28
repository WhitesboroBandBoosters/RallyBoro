package org.whitesborobandboosters.rallyboro.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.AgentTokenEntity;
import org.whitesborobandboosters.rallyboro.persistence.OffsetPage;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.AgentToken;
import org.whitesborobandboosters.rallyboro.staffing.Models.TokenGrant;

@Service
@Transactional(readOnly = true)
public class AgentTokens {
  private final PersistenceRepositories data;
  private final Access access;

  public AgentTokens(PersistenceRepositories data, Access access) {
    this.data = data;
    this.access = access;
  }

  private AgentToken details(AgentTokenEntity token) {
    return new AgentToken(
        token.getId(),
        token.getName(),
        token.getOrganizationId(),
        token.isWriteEnabled(),
        token.getExpiresAt().toString(),
        token.isRevoked());
  }

  public List<AgentToken> list(Actor actor) {
    access.session(actor);
    return data
        .tokens()
        .findByUserId(actor.id(), new OffsetPage(100, 0, Sort.by(Sort.Direction.DESC, "expiresAt")))
        .stream()
        .map(this::details)
        .toList();
  }

  public Actor authenticate(String raw) {
    var token =
        data.tokens()
            .findByTokenHashAndRevokedFalseAndExpiresAtAfter(
                SecurityConfig.digest(raw), Instant.now())
            .orElse(null);
    if (token == null) return null;
    var actor =
        new Actor(token.getUserId(), token.getOrganizationId(), true, token.isWriteEnabled());
    return access.member(actor, token.getOrganizationId()) ? actor : null;
  }

  @Transactional
  public TokenGrant create(Actor actor, UUID org, String name, boolean write) {
    access.session(actor);
    if (!access.member(actor, org)) Access.denied();
    if (name.isBlank() || name.length() > 80)
      throw new DomainException("INVALID_INPUT", "Token name must be 1–80 characters.");
    if (data.tokens().countByUserIdAndRevokedFalseAndExpiresAtAfter(actor.id(), Instant.now())
        >= 10)
      throw new DomainException(
          "LIMIT_EXCEEDED", "Revoke an existing token before creating another.");
    String raw = "rb_" + Accounts.secret();
    var token = new AgentTokenEntity(UUID.randomUUID());
    token.setUserId(actor.id());
    token.setOrganizationId(org);
    token.setName(name.strip());
    token.setTokenHash(SecurityConfig.digest(raw));
    token.setWriteEnabled(write);
    token.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    data.tokens().persist(token);
    return new TokenGrant(raw, details(token));
  }

  @Transactional
  public boolean revoke(Actor actor, UUID id) {
    access.session(actor);
    data.tokens()
        .findById(id)
        .filter(t -> t.getUserId().equals(actor.id()))
        .ifPresent(t -> t.setRevoked(true));
    return true;
  }
}
