package org.whitesborobandboosters.rallyboro;

import static org.assertj.core.api.Assertions.*;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.test.tester.ExecutionGraphQlServiceTester;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.EventInput;
import org.whitesborobandboosters.rallyboro.staffing.Models.JobInput;
import org.whitesborobandboosters.rallyboro.staffing.Models.ShiftInput;
import org.whitesborobandboosters.rallyboro.staffing.StaffingService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:18.7-bookworm:///staffing",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
      "spring.datasource.username=test",
      "spring.datasource.password=test",
      "rallyboro.pilot-login=true"
    })
class StaffingTest {
  @Autowired Environment environment;
  @Autowired JdbcTemplate db;
  @Autowired StaffingService service;
  @Autowired ExecutionGraphQlService execution;
  private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ORG2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID OWNER = Access.identity("local:organizer"),
      VOL = Access.identity("local:volunteer"),
      OTHER = Access.identity("local:other");
  private static final String PASSWORD = "synthetic-test-password";
  private final ObjectMapper json = new ObjectMapper();

  @DynamicPropertySource
  static void accounts(DynamicPropertyRegistry registry) throws Exception {
    Path file = Files.createTempFile("rallyboro-test-accounts-", ".properties");
    file.toFile().deleteOnExit();
    String hash = new BCryptPasswordEncoder(4).encode(PASSWORD);
    Files.writeString(
        file,
        "organizer.name=Organizer Test\norganizer.role=ORGANIZER\norganizer.password-hash="
            + hash
            + "\nvolunteer.name=Volunteer Test\nvolunteer.password-hash="
            + hash
            + "\nother.name=Other Test\nother.password-hash="
            + hash
            + "\n");
    registry.add("rallyboro.accounts-file", file::toString);
  }

  @BeforeEach
  void clear() {
    db.update("DELETE FROM mutation_receipt");
    db.update("DELETE FROM registration");
    db.update("DELETE FROM shift");
    db.update("DELETE FROM job");
    db.update("DELETE FROM event");
    db.update("DELETE FROM agent_token");
    db.update(
        "INSERT INTO organization(id,name) VALUES (?,'Second organization') ON CONFLICT DO NOTHING",
        ORG2);
  }

  private String url(String path) {
    return "http://localhost:" + environment.getProperty("local.server.port") + path;
  }

  private record Browser(HttpClient client, String csrf) {}

  private Browser login(String username) throws Exception {
    HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    String token =
        json.readTree(
                client
                    .send(
                        HttpRequest.newBuilder(URI.create(url("/auth/csrf"))).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                    .body())
            .get("token")
            .asText();
    var result =
        client.send(
            HttpRequest.newBuilder(URI.create(url("/auth/login")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-CSRF-TOKEN", token)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=" + username + "&password=" + PASSWORD))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(result.statusCode()).isEqualTo(204);
    String next =
        json.readTree(
                client
                    .send(
                        HttpRequest.newBuilder(URI.create(url("/auth/csrf"))).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                    .body())
            .get("token")
            .asText();
    return new Browser(client, next);
  }

  private JsonNode gql(Browser browser, String query, Map<String, Object> variables)
      throws Exception {
    var response =
        browser
            .client()
            .send(
                HttpRequest.newBuilder(URI.create(url("/graphql")))
                    .header("Content-Type", "application/json")
                    .header("X-CSRF-TOKEN", browser.csrf())
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(
                                Map.of("query", query, "variables", variables))))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    return json.readTree(response.body());
  }

  private Actor actor(UUID user) {
    return new Actor(user, null, false, true);
  }

  private UUID fixture(int capacity, int hour) {
    String date = LocalDate.now().plusDays(10).toString();
    var event =
        service.saveEvent(
            actor(OWNER),
            new EventInput(
                null,
                ORG,
                "Community supper",
                "Synthetic event",
                "Community hall",
                date,
                "UTC",
                "PRIVATE",
                false,
                null));
    var job = service.saveJob(actor(OWNER), new JobInput(null, event.id(), "Welcome team", "", ""));
    UUID shift =
        service
            .saveShift(
                actor(OWNER),
                new ShiftInput(
                    null,
                    job.id(),
                    date + String.format("T%02d:00:00Z", hour),
                    date + String.format("T%02d:00:00Z", hour + 1),
                    capacity))
            .id();
    service.saveEvent(
        actor(OWNER),
        new EventInput(
            event.id(),
            ORG,
            event.title(),
            event.description(),
            event.location(),
            date,
            "UTC",
            "PRIVATE",
            true,
            event.revision()));
    return shift;
  }

  private String signup(UUID shift, UUID request) {
    return "mutation {signup(shiftId:\"" + shift + "\",requestId:\"" + request + "\"){id status}}";
  }

  @Test
  void signup_competingGraphqlRequests_confirmsOnlyOneLastPlace() throws Exception {
    UUID shift = fixture(1, 10);
    Browser a = login("volunteer"), b = login("other");
    CyclicBarrier barrier = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(() -> {
            barrier.await();
            return gql(a, signup(shift, UUID.randomUUID()), Map.of());
          });
      var second =
          executor.submit(() -> {
            barrier.await();
            return gql(b, signup(shift, UUID.randomUUID()), Map.of());
          });
      List<JsonNode> results =
          List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
      assertThat(results.stream().filter(r -> !r.has("errors")).count()).isEqualTo(1);
      assertThat(results.stream().filter(r -> r.has("errors")).findFirst().orElseThrow().toString())
          .contains("FULL");
    }
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM registration WHERE status='CONFIRMED'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void signup_retriedRequest_returnsSameAssignmentAndDoesNotResurrectCancellation()
      throws Exception {
    UUID shift = fixture(2, 10), request = UUID.randomUUID();
    Browser browser = login("volunteer");
    var first = gql(browser, signup(shift, request), Map.of());
    var second = gql(browser, signup(shift, request), Map.of());
    assertThat(second).isEqualTo(first);
    String id = first.at("/data/signup/id").asText();
    var cancelled =
        gql(browser, "mutation {cancelRegistration(id:\"" + id + "\"){status}}", Map.of());
    assertThat(cancelled.toString()).contains("CANCELLED");
    assertThat(gql(browser, signup(shift, request), Map.of()).at("/data/signup/status").asText())
        .isEqualTo("CANCELLED");
  }

  @Test
  void signup_concurrentOverlappingEvents_preventsDoubleCommitment() throws Exception {
    UUID a = fixture(2, 10), b = fixture(2, 10);
    Browser one = login("volunteer"), two = login("volunteer");
    try (var executor = Executors.newFixedThreadPool(2)) {
      var f = executor.submit(() -> gql(one, signup(a, UUID.randomUUID()), Map.of()));
      var g = executor.submit(() -> gql(two, signup(b, UUID.randomUUID()), Map.of()));
      var results = List.of(f.get(20, TimeUnit.SECONDS), g.get(20, TimeUnit.SECONDS));
      assertThat(results.stream().filter(r -> !r.has("errors")).count()).isEqualTo(1);
      assertThat(results.toString()).contains("OVERLAP");
    }
  }

  @Test
  void access_crossOrganizationAndCoverage_doNotExposeRecords() throws Exception {
    UUID shift = fixture(2, 10);
    Browser volunteer = login("volunteer");
    UUID event =
        service.myRegistrations(actor(VOL), 10, 0).stream()
            .findFirst()
            .map(r -> r.event().id())
            .orElseGet(() ->
                db.queryForObject(
                    "SELECT event_id FROM job j JOIN shift s ON s.job_id=j.id WHERE s.id=?",
                    UUID.class,
                    shift));
    assertThat(
            gql(
                    volunteer,
                    "{coverage(eventId:\"" + event + "\"){participantName event{title}}}",
                    Map.of())
                .toString())
        .contains("FORBIDDEN")
        .doesNotContain("Organizer Test");
    assertThat(
            gql(
                    volunteer,
                    "{events(organizationId:\""
                        + ORG2
                        + "\"){items{title jobs{shifts{capacity}}}}}",
                    Map.of())
                .toString())
        .contains("FORBIDDEN");
    var result = gql(volunteer, "{viewer { id email }}", Map.of());
    assertThat(result.toString()).contains("errors");
  }

  @Test
  void edits_existingAssignmentsAndStaleRevision_rejectUnsafeChanges() {
    UUID shift = fixture(2, 10);
    service.signup(actor(VOL), shift, VOL, UUID.randomUUID(), false);
    UUID event =
        db.queryForObject(
            "SELECT event_id FROM job j JOIN shift s ON s.job_id=j.id WHERE s.id=?",
            UUID.class,
            shift);
    var e = service.event(actor(OWNER), event);
    assertThatThrownBy(() ->
            service.saveEvent(
                actor(OWNER),
                new EventInput(
                    e.id(), ORG, e.title(), "", "", e.eventDate(), "UTC", "PRIVATE", true, -1)))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("Reload");
    assertThatThrownBy(
            () -> service.saveJob(actor(OWNER), new JobInput(null, event, "New role", "", "")))
        .hasMessageContaining("Remove confirmed");
    assertThatThrownBy(() ->
            service.cancel(
                actor(OTHER), service.myRegistrations(actor(VOL), 10, 0).getFirst().id(), false))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void transport_missingCsrfAndMalformedGraphql_rejectsSafely() throws Exception {
    Browser browser = login("volunteer");
    var response =
        browser
            .client()
            .send(
                HttpRequest.newBuilder(URI.create(url("/graphql")))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{viewer{id}}\"}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(403);
    assertThat(gql(browser, "{ definitelyNotAField }", Map.of()).toString())
        .contains("errors")
        .doesNotContain("jdbc:", "password-hash");
    assertThat(
            gql(browser, "{events(organizationId:\"" + ORG + "\",first:1000){total}}", Map.of())
                .toString())
        .contains("INVALID_INPUT");
  }

  @Test
  void organizer_assignmentRemovalAndClosedSignup_enforceSameRules() throws Exception {
    UUID shift = fixture(1, 10);
    Browser organizer = login("organizer");
    var result =
        gql(
            organizer,
            "mutation{assignVolunteer(shiftId:\""
                + shift
                + "\",userId:\""
                + VOL
                + "\",requestId:\""
                + UUID.randomUUID()
                + "\"){id status}}",
            Map.of());
    String id = result.at("/data/assignVolunteer/id").asText();
    assertThat(result.toString()).doesNotContain("errors");
    assertThat(
            gql(organizer, "mutation{removeAssignment(id:\"" + id + "\"){status}}", Map.of())
                .toString())
        .contains("CANCELLED");
    db.update("UPDATE event SET signup_open=false");
    assertThat(gql(login("other"), signup(shift, UUID.randomUUID()), Map.of()).toString())
        .contains("CLOSED");
  }

  @Test
  void agent_readOnlyTokenAndRevocation_limitPermissions() throws Exception {
    UUID shift = fixture(2, 10);
    Browser browser = login("volunteer");
    var grant =
        gql(
            browser,
            "mutation{createAgentToken(organizationId:\""
                + ORG
                + "\",name:\"Test agent\",writeEnabled:false){token details{id}}}",
            Map.of());
    String token = grant.at("/data/createAgentToken/token").asText(),
        id = grant.at("/data/createAgentToken/details/id").asText();
    assertThat(token).startsWith("rb_");
    var client = HttpClient.newHttpClient();
    var response =
        client.send(
            HttpRequest.newBuilder(URI.create(url("/graphql")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        json.writeValueAsString(Map.of("query", signup(shift, UUID.randomUUID())))))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("FORBIDDEN");
    gql(browser, "mutation{revokeAgentToken(id:\"" + id + "\")}", Map.of());
    var revoked =
        client.send(
            HttpRequest.newBuilder(URI.create(url("/graphql")))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{viewer{id}}\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertThat(revoked.statusCode()).isEqualTo(401);
    assertThat(
            db.queryForObject(
                "SELECT token_hash FROM agent_token WHERE id=?", String.class, UUID.fromString(id)))
        .isNotEqualTo(token);
  }

  @Test
  void schema_statusOperation_satisfiesCheckedInContract() {
    ExecutionGraphQlServiceTester.create(execution)
        .document("{appInfo{name stage}}")
        .execute()
        .path("appInfo.stage")
        .entity(String.class)
        .isEqualTo("staffing-pilot");
  }

  @Test
  void signup_backToBackShiftsAndCapacityRelease_remainUsable() throws Exception {
    UUID first = fixture(1, 10), second = fixture(1, 11);
    Browser volunteer = login("volunteer"), other = login("other");
    var a = gql(volunteer, signup(first, UUID.randomUUID()), Map.of());
    assertThat(gql(volunteer, signup(second, UUID.randomUUID()), Map.of()).toString())
        .doesNotContain("errors");
    String id = a.at("/data/signup/id").asText();
    gql(volunteer, "mutation{cancelRegistration(id:\"" + id + "\"){status}}", Map.of());
    assertThat(gql(other, signup(first, UUID.randomUUID()), Map.of()).toString())
        .doesNotContain("errors");
  }

  @Test
  void requests_complexityBatchAndAnonymousAccess_areBounded() throws Exception {
    Browser browser = login("volunteer");
    String aliases =
        "{ "
            + java.util.stream.IntStream.range(0, 1100)
                .mapToObj(i -> "a" + i + ":appInfo{name}")
                .collect(java.util.stream.Collectors.joining(" "))
            + " }";
    assertThat(gql(browser, aliases, Map.of()).toString()).contains("errors");
    HttpClient anonymousClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    String token =
        json.readTree(
                anonymousClient
                    .send(
                        HttpRequest.newBuilder(URI.create(url("/auth/csrf"))).GET().build(),
                        HttpResponse.BodyHandlers.ofString())
                    .body())
            .path("token")
            .asText();
    assertThat(
            gql(new Browser(anonymousClient, token), "{viewer{id displayName}}", Map.of())
                .toString())
        .contains("UNAUTHENTICATED")
        .doesNotContain("Synthetic organizer");
    var batch =
        browser
            .client()
            .send(
                HttpRequest.newBuilder(URI.create(url("/graphql")))
                    .header("Content-Type", "application/json")
                    .header("X-CSRF-TOKEN", browser.csrf())
                    .POST(HttpRequest.BodyPublishers.ofString("[{\"query\":\"{appInfo{name}}\"}]"))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(batch.statusCode()).isEqualTo(400);
  }

  @Test
  void cancellation_delayedRetry_doesNotCancelLaterBooking() throws Exception {
    UUID shift = fixture(1, 10);
    Browser browser = login("volunteer");
    var first = gql(browser, signup(shift, UUID.randomUUID()), Map.of());
    String old = first.at("/data/signup/id").asText();
    String cancel = "mutation{cancelRegistration(id:\"" + old + "\"){status}}";
    gql(browser, cancel, Map.of());
    var next = gql(browser, signup(shift, UUID.randomUUID()), Map.of());
    String current = next.at("/data/signup/id").asText();
    assertThat(current).isNotEqualTo(old);
    gql(browser, cancel, Map.of());
    assertThat(
            db.queryForObject(
                "SELECT status FROM registration WHERE id=?",
                String.class,
                UUID.fromString(current)))
        .isEqualTo("CONFIRMED");
  }

  @Test
  void event_rescheduleWithExistingShifts_rejectsDateMismatch() {
    UUID shift = fixture(2, 10);
    UUID id =
        db.queryForObject(
            "SELECT event_id FROM job j JOIN shift s ON s.job_id=j.id WHERE s.id=?",
            UUID.class,
            shift);
    var e = service.event(actor(OWNER), id);
    assertThatThrownBy(() ->
            service.saveEvent(
                actor(OWNER),
                new EventInput(
                    e.id(),
                    ORG,
                    e.title(),
                    e.description(),
                    e.location(),
                    LocalDate.parse(e.eventDate()).plusDays(1).toString(),
                    e.timezone(),
                    e.visibility(),
                    e.signupOpen(),
                    e.revision())))
        .isInstanceOf(DomainException.class)
        .hasMessageContaining("fixed after shifts");
  }
}
