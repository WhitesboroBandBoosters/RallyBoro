package org.whitesborobandboosters.rallyboro.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AgentTokenRepository extends LockedRepository<AgentTokenEntity, UUID> {
  List<AgentTokenEntity> findByUserId(UUID user);

  List<AgentTokenEntity> findByUserIdAndOrganizationId(UUID user, UUID org);

  List<AgentTokenEntity> findByUserId(UUID user, Pageable page);

  long countByUserIdAndRevokedFalseAndExpiresAtAfter(UUID user, Instant now);

  Optional<AgentTokenEntity> findByTokenHashAndRevokedFalseAndExpiresAtAfter(
      String hash, Instant now);
}
