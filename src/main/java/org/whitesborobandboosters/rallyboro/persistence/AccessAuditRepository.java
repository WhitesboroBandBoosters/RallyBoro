package org.whitesborobandboosters.rallyboro.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface AccessAuditRepository extends LockedRepository<AccessAuditEntity, UUID> {
  List<AccessAuditEntity> findByOrganizationId(UUID org, Pageable page);
}
