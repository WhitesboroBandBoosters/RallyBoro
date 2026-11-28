package org.whitesborobandboosters.rallyboro.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface MembershipRepository extends LockedRepository<MembershipEntity, MembershipId> {
  List<MembershipEntity> findByIdUserId(UUID user);

  List<MembershipEntity> findByIdOrganizationId(UUID org, Pageable page);

  long countByIdUserIdAndCanManageTrue(UUID user);

  long countByIdOrganizationIdAndCanManageTrue(UUID org);

  boolean existsByIdUserIdAndIdOrganizationIdAndCanManageTrue(UUID user, UUID org);
}
