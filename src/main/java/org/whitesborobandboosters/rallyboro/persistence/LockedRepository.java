package org.whitesborobandboosters.rallyboro.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface LockedRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
  T persist(T entity);

  Optional<T> lockById(ID id);
}
