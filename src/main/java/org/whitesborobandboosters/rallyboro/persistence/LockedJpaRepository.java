package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

public class LockedJpaRepository<T, ID> extends SimpleJpaRepository<T, ID>
    implements LockedRepository<T, ID> {
  private final EntityManager entities;
  private final Class<T> type;

  public LockedJpaRepository(JpaEntityInformation<T, ?> info, EntityManager entities) {
    super(info, entities);
    this.entities = entities;
    this.type = info.getJavaType();
  }

  @Override
  @Transactional
  public T persist(T entity) {
    entities.persist(entity);
    return entity;
  }

  @Override
  @Transactional
  public Optional<T> lockById(ID id) {
    T entity = entities.find(type, id, LockModeType.PESSIMISTIC_WRITE);
    // A prior non-locking lookup may have cached state before a competing transaction committed.
    if (entity != null) entities.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
    return Optional.ofNullable(entity);
  }
}
