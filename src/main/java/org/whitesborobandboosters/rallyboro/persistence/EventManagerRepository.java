package org.whitesborobandboosters.rallyboro.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface EventManagerRepository
    extends LockedRepository<EventManagerEntity, EventManagerId> {
  boolean existsByIdUserIdAndEventOrganizationId(UUID user, UUID org);

  List<EventManagerEntity> findByIdUserIdAndEventOrganizationId(UUID user, UUID org);

  List<EventManagerEntity> findByIdEventId(UUID event, Pageable page);
}
