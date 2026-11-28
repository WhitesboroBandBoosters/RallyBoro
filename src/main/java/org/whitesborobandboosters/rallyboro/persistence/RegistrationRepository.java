package org.whitesborobandboosters.rallyboro.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

public interface RegistrationRepository extends LockedRepository<RegistrationEntity, UUID> {
  boolean existsByParticipantUserIdAndShiftJobEventId(UUID user, UUID event);

  boolean existsByShiftJobEventIdAndStatus(UUID event, String status);

  long countByShiftIdAndStatus(UUID shift, String status);

  Optional<RegistrationEntity> findByParticipantIdAndShiftIdAndStatus(
      UUID person, UUID shift, String status);

  boolean existsByParticipantIdAndStatusAndShiftStartsAtLessThanAndShiftEndsAtGreaterThan(
      UUID person, String status, Instant end, Instant start);

  @EntityGraph(attributePaths = "shift")
  List<RegistrationEntity>
      findByParticipantUserIdAndStatusAndShiftStartsAtLessThanAndShiftEndsAtGreaterThan(
          UUID user, String status, Instant end, Instant start);

  List<RegistrationEntity> findByShiftJobEventIdAndStatus(UUID event, String status, Pageable page);
}
