package org.whitesborobandboosters.rallyboro.persistence;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends LockedRepository<CredentialEntity, UUID> {
  Optional<CredentialEntity> findByUsername(String name);

  boolean existsByUsername(String name);
}
