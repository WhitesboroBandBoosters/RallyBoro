package org.whitesborobandboosters.rallyboro.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface InvitationRepository extends LockedRepository<InvitationEntity, UUID> {
  Optional<InvitationEntity> findByTokenHash(String hash);

  long countByOrganizationIdAndRevokedFalseAndAcceptedByIsNullAndExpiresAtAfter(
      UUID org, Instant now);

  List<InvitationEntity> findByOrganizationId(UUID org, Pageable page);

  List<InvitationEntity> findByOrganizationIdAndCreatedByAndAcceptedByIsNull(UUID org, UUID user);
}
