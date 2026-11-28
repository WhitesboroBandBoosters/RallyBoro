package org.whitesborobandboosters.rallyboro;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PersistenceArchitectureTest {
  @Test
  void applicationPersistence_javaSources_useOrmWithoutTextQueries() throws Exception {
    var forbidden =
        Pattern.compile(
            "JdbcTemplate|NamedParameterJdbcTemplate|createNativeQuery|createQuery\\s*\\(\\s*\"|@Query\\s*\\(");
    var violations = new ArrayList<String>();
    try (var sources = Files.walk(Path.of("src/main/java"))) {
      for (var source : sources.filter(p -> p.toString().endsWith(".java")).toList())
        if (forbidden.matcher(Files.readString(source)).find()) violations.add(source.toString());
    }
    assertThat(violations)
        .as(
            "Application persistence must use typed JPA repositories/Criteria; SQL belongs in"
                + " migrations or independent test setup")
        .isEmpty();
  }
}
