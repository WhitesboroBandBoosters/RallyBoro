package org.whitesborobandboosters.rallyboro.persistence;

import java.util.UUID;

public interface EventRepository extends LockedRepository<EventEntity, UUID> {
  boolean existsByIdAndOrganizationId(UUID id, UUID org);
}
