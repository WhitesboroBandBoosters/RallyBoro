package org.whitesborobandboosters.rallyboro.persistence;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends LockedRepository<ParticipantEntity, UUID> {
  Optional<ParticipantEntity> findByUserId(UUID user);
}
