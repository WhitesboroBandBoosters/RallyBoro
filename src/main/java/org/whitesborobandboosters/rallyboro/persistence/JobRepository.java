package org.whitesborobandboosters.rallyboro.persistence;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends LockedRepository<JobEntity, UUID> {
  List<JobEntity> findByEventIdOrderById(UUID event);

  long countByEventId(UUID event);
}
