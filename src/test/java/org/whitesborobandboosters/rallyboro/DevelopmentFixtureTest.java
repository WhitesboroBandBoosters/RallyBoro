package org.whitesborobandboosters.rallyboro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:18.7-bookworm:///development_fixture",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
      "spring.datasource.username=test",
      "spring.datasource.password=test",
      "spring.profiles.active=development",
      "rallyboro.fixture.enabled=true"
    })
class DevelopmentFixtureTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired DevelopmentFixture fixture;

  @Test
  void fixture_repeatedStartup_preservesSingleSyntheticRecord() {
    fixture.run(null);
    fixture.run(null);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM foundation_probe WHERE id='development-fixture'",
                Integer.class))
        .isEqualTo(1);
  }
}
