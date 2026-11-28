package org.whitesborobandboosters.rallyboro.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(repositoryBaseClass = LockedJpaRepository.class)
public class PersistenceConfig {}
