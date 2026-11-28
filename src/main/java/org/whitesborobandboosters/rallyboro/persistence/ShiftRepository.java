package org.whitesborobandboosters.rallyboro.persistence;

import java.util.List;
import java.util.UUID;

public interface ShiftRepository extends LockedRepository<ShiftEntity, UUID> {
  List<ShiftEntity> findByJobIdOrderByStartsAtAscIdAsc(UUID job);

  List<ShiftEntity> findByJobEventIdOrderByStartsAtAscIdAsc(UUID event);

  long countByJobId(UUID job);

  long countByJobEventId(UUID event);
}
