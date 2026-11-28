package org.whitesborobandboosters.rallyboro;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:18.7-bookworm:///foundation",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
      "spring.datasource.username=test",
      "spring.datasource.password=test",
      "spring.profiles.active=production",
      "rallyboro.fixture.enabled=true"
    })
class FoundationTest {
  @Autowired Environment environment;
  @Autowired JdbcTemplate jdbc;

  private HttpResponse<String> get(String path) throws Exception {
    return HttpClient.newHttpClient()
        .send(
            HttpRequest.newBuilder(
                    URI.create(
                        "http://localhost:" + environment.getProperty("local.server.port") + path))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Test
  void health_databaseAvailable_returnsUpWithoutDetails() throws Exception {
    var response = get("/actuator/health/readiness");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("UP").doesNotContain("components", "password", "jdbc:");
  }

  @Test
  void fixture_productionProfile_doesNotInsertEvenWhenEnabled() {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM foundation_probe", Integer.class))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class))
        .isEqualTo(4);
  }

  @Test
  void home_request_rendersResponsivePage() throws Exception {
    var response = get("/");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("RallyBoro", "width=device-width").doesNotContain("jdbc:");
    var stylesheet = Pattern.compile("href=\"([^\"]+\\.css)\"").matcher(response.body());
    assertThat(stylesheet.find()).isTrue();
    assertThat(get(stylesheet.group(1)).statusCode()).isEqualTo(200);
  }

  @Test
  void status_request_returnsStableContract() throws Exception {
    var response = get("/api/v1/status");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"name\":\"RallyBoro\"", "\"stage\":\"staffing-pilot\"");
  }

  @Test
  void diagnostics_unexposedPaths_returnNotFound() throws Exception {
    for (String path : new String[] {"/actuator/env", "/actuator/configprops", "/api/v1/fixture"}) {
      assertThat(get(path).statusCode()).isIn(401, 403, 404);
    }
  }
}
