package org.whitesborobandboosters.rallyboro;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("development & !production")
@ConditionalOnProperty(name = "rallyboro.fixture.enabled", havingValue = "true")
public class DevelopmentFixture implements ApplicationRunner {
  private final JdbcTemplate jdbc;

  public DevelopmentFixture(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void run(ApplicationArguments args) {
    jdbc.update(
        """
        INSERT INTO foundation_probe (id, description)
        VALUES ('development-fixture', 'Synthetic persistence verification')
        ON CONFLICT (id) DO NOTHING
        """);
  }
}
