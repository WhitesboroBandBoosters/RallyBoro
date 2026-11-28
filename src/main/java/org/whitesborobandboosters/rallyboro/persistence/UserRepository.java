package org.whitesborobandboosters.rallyboro.persistence;

import java.util.UUID;

public interface UserRepository extends LockedRepository<UserEntity, UUID> {}
