package org.whitesborobandboosters.rallyboro;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.ProbeEntity;
import org.whitesborobandboosters.rallyboro.persistence.ProbeRepository;

@Component
@Profile("development & !production")
@ConditionalOnProperty(name = "rallyboro.fixture.enabled", havingValue = "true")
public class DevelopmentFixture implements ApplicationRunner {
  private final ProbeRepository probes;

  public DevelopmentFixture(ProbeRepository probes) {
    this.probes = probes;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!probes.existsById("development-fixture")) {
      var probe = new ProbeEntity("development-fixture");
      probe.setDescription("Synthetic persistence verification");
      probes.persist(probe);
    }
  }
}
