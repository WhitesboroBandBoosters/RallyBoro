package org.whitesborobandboosters.rallyboro;

import static org.assertj.core.api.Assertions.*;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.whitesborobandboosters.rallyboro.organization.Organizations;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.security.Accounts;
import org.whitesborobandboosters.rallyboro.security.AgentTokens;
import org.whitesborobandboosters.rallyboro.staffing.Models.EventInput;
import org.whitesborobandboosters.rallyboro.staffing.Models.JobInput;
import org.whitesborobandboosters.rallyboro.staffing.Models.ShiftInput;
import org.whitesborobandboosters.rallyboro.staffing.StaffingService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:tc:postgresql:18.7-bookworm:///accounts",
      "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
      "spring.datasource.username=test",
      "spring.datasource.password=test",
      "rallyboro.account-attempts-per-minute=1000"
    })
class AccountsAndOrganizationsTest {
  @TestConfiguration
  static class FastPasswords {
    @Bean
    @Primary
    PasswordEncoder testPasswordEncoder() {
      return new BCryptPasswordEncoder(4);
    }
  }

  @Autowired JdbcTemplate db;
  @Autowired PersistenceRepositories data;
  @Autowired Environment environment;
  @Autowired Accounts accounts;
  @Autowired Organizations organizations;
  @Autowired StaffingService staffing;
  @Autowired Access access;
  @Autowired AgentTokens tokens;
  private final ObjectMapper json = new ObjectMapper();
  private static final String PASSWORD = "synthetic-password-only";
  private static final String NEW_PASSWORD = "replacement-synthetic-password";

  private record Browser(HttpClient client, String csrf) {}

  private record Person(String username, UUID id, String recoveryKey, Browser browser) {
    Actor actor() {
      return new Actor(id, null, false, true);
    }
  }

  private String url(String path) {
    return "http://localhost:" + environment.getProperty("local.server.port") + path;
  }

  private Browser browser() throws Exception {
    HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    return new Browser(client, csrf(client));
  }

  private String csrf(HttpClient client) throws Exception {
    return json.readTree(
            client
                .send(
                    HttpRequest.newBuilder(URI.create(url("/auth/csrf"))).GET().build(),
                    HttpResponse.BodyHandlers.ofString())
                .body())
        .get("token")
        .asText();
  }

  private int loginStatus(Browser browser, String username, String password) throws Exception {
    return browser
        .client()
        .send(
            HttpRequest.newBuilder(URI.create(url("/auth/login")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("X-CSRF-TOKEN", browser.csrf())
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "username=" + username + "&password=" + password))
                .build(),
            HttpResponse.BodyHandlers.ofString())
        .statusCode();
  }

  private Browser login(String username, String password) throws Exception {
    var browser = browser();
    assertThat(loginStatus(browser, username, password)).isEqualTo(204);
    return new Browser(browser.client(), csrf(browser.client()));
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

  private Person person() throws Exception {
    String name = "user_" + UUID.randomUUID().toString().replace("-", "");
    var created = accounts.register(name, "Synthetic member", PASSWORD, "direct-tests");
    return new Person(name, created.id(), created.recoveryKey(), login(name, PASSWORD));
  }

  private UUID organization(Person person) {
    return organizations.create(person.actor(), "Synthetic organization").id();
  }

  private UUID event(Person owner, UUID org, String title) {
    return staffing
        .saveEvent(
            owner.actor(),
            new EventInput(
                null,
                org,
                title,
                "",
                "",
                LocalDate.now().plusDays(10).toString(),
                "UTC",
                "HIDDEN",
                false,
                null))
        .id();
  }

  private JsonNode accept(Browser browser, String token) throws Exception {
    return gql(
        browser,
        "mutation($token:String!){acceptInvitation(token:$token){id}}",
        Map.of("token", token));
  }

  private JsonNode recover(Browser browser, Person person, String key) throws Exception {
    return gql(
        browser,
        "mutation($username:String!,$key:String!,$password:String!){recoverAccount(username:$username,recoveryKey:$key,newPassword:$password)}",
        Map.of("username", person.username(), "key", key, "password", NEW_PASSWORD));
  }

  @Test
  void registerAccount_newUsername_createsStableIdentityWithoutOrganization() throws Exception {
    var anonymous = browser();
    String name = "new_" + UUID.randomUUID().toString().replace("-", "");
    var result =
        gql(
            anonymous,
            "mutation($u:String!,$p:String!){registerAccount(username:$u,displayName:\"New"
                + " member\",password:$p){id recoveryKey}}",
            Map.of("u", name, "p", PASSWORD));
    assertThat(result.has("errors")).isFalse();
    UUID id = UUID.fromString(result.at("/data/registerAccount/id").asText());
    assertThat(id).isNotEqualTo(Access.identity("local:" + name));
    var loggedIn = login(name.toUpperCase(), PASSWORD);
    var viewer = gql(loggedIn, "{viewer{id memberships{organizationId}}}", Map.of());
    assertThat(viewer.at("/data/viewer/id").asText()).isEqualTo(id.toString());
    assertThat(viewer.at("/data/viewer/memberships").size()).isZero();
    var credential = db.queryForMap("SELECT * FROM local_credential WHERE user_id=?", id);
    assertThat(credential.get("password_hash").toString())
        .startsWith("$2")
        .doesNotContain(PASSWORD);
    assertThat(credential.get("recovery_hash"))
        .isNotEqualTo(result.at("/data/registerAccount/recoveryKey").asText());
    assertThat(
            gql(
                    browser(),
                    "mutation($u:String!,$p:String!){registerAccount(username:$u,displayName:\"Other\",password:$p){id}}",
                    Map.of("u", name.toUpperCase(), "p", PASSWORD))
                .toString())
        .contains("CONFLICT");
    assertThat(
            gql(loggedIn, "mutation{createOrganization(name:\"My organization\"){id}}", Map.of())
                .has("errors"))
        .isFalse();
    assertThat(
            gql(loggedIn, "{viewer{memberships{canManage canOrganize}}}", Map.of())
                .at("/data/viewer/memberships/0/canManage")
                .asBoolean())
        .isTrue();
  }

  @Test
  void registerAccount_invalidCredentialsAndMissingCsrf_rejectsWithoutPartialUser()
      throws Exception {
    int before = db.queryForObject("SELECT count(*) FROM app_user", Integer.class);
    var result =
        gql(
            browser(),
            "mutation{registerAccount(username:\"bad"
                + " name\",displayName:\"Name\",password:\"short\"){id}}",
            Map.of());
    assertThat(result.toString()).contains("INVALID_INPUT");
    assertThat(db.queryForObject("SELECT count(*) FROM app_user", Integer.class)).isEqualTo(before);
    String query =
        "mutation{registerAccount(username:\"valid_name\",displayName:\"Name\",password:\"short\"){id}}";
    assertThat(gql(browser(), query, Map.of()).toString()).contains("INVALID_INPUT");
    var response =
        HttpClient.newHttpClient()
            .send(
                HttpRequest.newBuilder(URI.create(url("/graphql")))
                    .header("Content-Type", "application/json")
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            json.writeValueAsString(Map.of("query", query))))
                    .build(),
                HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(403);
  }

  @Test
  void recoverAccount_validKey_rotatesKeyAndInvalidatesSessionsAndTokens() throws Exception {
    Person person = person();
    UUID org = organization(person);
    tokens.create(person.actor(), org, "Synthetic token", true);
    var reset = recover(browser(), person, person.recoveryKey());
    assertThat(reset.has("errors")).isFalse();
    String replacement = reset.at("/data/recoverAccount").asText();
    assertThat(replacement).hasSize(43).isNotEqualTo(person.recoveryKey());
    assertThat(gql(person.browser(), "{viewer{id}}", Map.of()).toString())
        .contains("UNAUTHENTICATED");
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM agent_token WHERE user_id=? AND NOT revoked",
                Integer.class,
                person.id()))
        .isZero();
    assertThat(loginStatus(browser(), person.username(), PASSWORD)).isEqualTo(401);
    assertThat(
            gql(login(person.username(), NEW_PASSWORD), "{viewer{id}}", Map.of())
                .at("/data/viewer/id")
                .asText())
        .isEqualTo(person.id().toString());
    assertThat(recover(browser(), person, person.recoveryKey()).toString()).contains("FORBIDDEN");
  }

  @Test
  void recoverAccount_concurrentSameKey_allowsOnlyOneReset() throws Exception {
    Person person = person();
    Browser a = browser(), b = browser();
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(() -> {
            barrier.await();
            return recover(a, person, person.recoveryKey());
          });
      var second =
          executor.submit(() -> {
            barrier.await();
            return recover(b, person, person.recoveryKey());
          });
      var one = first.get(20, TimeUnit.SECONDS);
      var two = second.get(20, TimeUnit.SECONDS);
      assertThat((one.has("errors") ? 0 : 1) + (two.has("errors") ? 0 : 1)).isEqualTo(1);
    }
  }

  @Test
  void changePassword_wrongThenCorrectPassword_requiresProofAndInvalidatesOldSession()
      throws Exception {
    Person person = person();
    String query =
        "mutation($old:String!,$next:String!){changePassword(currentPassword:$old,newPassword:$next)}";
    assertThat(
            gql(person.browser(), query, Map.of("old", "incorrect", "next", NEW_PASSWORD))
                .toString())
        .contains("FORBIDDEN");
    assertThat(
            gql(person.browser(), query, Map.of("old", PASSWORD, "next", NEW_PASSWORD))
                .has("errors"))
        .isFalse();
    assertThat(gql(person.browser(), "{viewer{id}}", Map.of()).toString())
        .contains("UNAUTHENTICATED");
    assertThat(loginStatus(browser(), person.username(), NEW_PASSWORD)).isEqualTo(204);
  }

  @Test
  void rotateRecoveryKey_currentPassword_replacesKeyAndPreservesAccount() throws Exception {
    Person person = person();
    var rotated =
        gql(
            person.browser(),
            "mutation($p:String!){rotateRecoveryKey(currentPassword:$p)}",
            Map.of("p", PASSWORD));
    assertThat(rotated.has("errors")).isFalse();
    String key = rotated.at("/data/rotateRecoveryKey").asText();
    assertThat(key).hasSize(43).isNotEqualTo(person.recoveryKey());
    assertThat(gql(person.browser(), "{viewer{id}}", Map.of()).at("/data/viewer/id").asText())
        .isEqualTo(person.id().toString());
    assertThat(recover(browser(), person, person.recoveryKey()).toString()).contains("FORBIDDEN");
    assertThat(recover(browser(), person, key).has("errors")).isFalse();
  }

  @Test
  void accountLimits_repeatedInvalidRequestsAndDisabledAccounts_rejectBeforeWriting() {
    var limited = new Accounts(data, new BCryptPasswordEncoder(4), true, 1);
    assertThatThrownBy(() -> limited.register("bad name", "Name", PASSWORD, "same-address"))
        .hasMessageContaining("Username");
    assertThatThrownBy(() -> limited.register("valid_name", "Name", PASSWORD, "same-address"))
        .hasMessageContaining("Too many");
    var disabled = new Accounts(data, new BCryptPasswordEncoder(4), false, 10);
    assertThatThrownBy(() -> disabled.register("valid_name", "Name", PASSWORD, "address"))
        .hasMessageContaining("disabled");
    assertThatThrownBy(() ->
            accounts.register(
                "utf8_" + UUID.randomUUID().toString().replace("-", ""),
                "Name",
                "😀".repeat(19),
                "boundary"))
        .hasMessageContaining("72 UTF-8 bytes");
  }

  @Test
  void acceptInvitation_concurrentDifferentAccounts_consumesOnceWithoutAutomaticMembership()
      throws Exception {
    Person owner = person(), a = person(), b = person();
    UUID org = organization(owner);
    assertThat(access.member(a.actor(), org)).isFalse();
    var invitation = organizations.invite(owner.actor(), org, false, false, null);
    assertThat(
            db.queryForObject(
                "SELECT token_hash FROM organization_invitation WHERE id=?",
                String.class,
                invitation.invitation().id()))
        .isNotEqualTo(invitation.token());
    assertThat(
            gql(
                    browser(),
                    "query($t:String!){invitation(token:$t){organizationName}}",
                    Map.of("t", invitation.token()))
                .toString())
        .doesNotContain("Synthetic organization");
    var barrier = new CyclicBarrier(2);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(() -> {
            barrier.await();
            return accept(a.browser(), invitation.token());
          });
      var second =
          executor.submit(() -> {
            barrier.await();
            return accept(b.browser(), invitation.token());
          });
      JsonNode one = first.get(20, TimeUnit.SECONDS), two = second.get(20, TimeUnit.SECONDS);
      assertThat((one.has("errors") ? 0 : 1) + (two.has("errors") ? 0 : 1)).isEqualTo(1);
      Person winner = one.has("errors") ? b : a;
      assertThat(accept(winner.browser(), invitation.token()).has("errors")).isFalse();
      assertThat(access.organizer(winner.actor(), org)).isFalse();
    }
  }

  @Test
  void acceptInvitation_revokedExpiredOrFormerManager_rejectsWithoutGrantingAccess()
      throws Exception {
    Person owner = person(), guest = person(), manager = person();
    UUID org = organization(owner);
    var revoked = organizations.invite(owner.actor(), org, false, true, null);
    organizations.revoke(owner.actor(), org, revoked.invitation().id());
    assertThat(accept(guest.browser(), revoked.token()).toString()).contains("NOT_FOUND");
    var expired = organizations.invite(owner.actor(), org, false, true, null);
    db.update(
        "UPDATE organization_invitation SET expires_at=now()-interval '1 second' WHERE id=?",
        expired.invitation().id());
    assertThat(accept(guest.browser(), expired.token()).toString()).contains("NOT_FOUND");
    organizations.accept(
        manager.actor(), organizations.invite(owner.actor(), org, true, true, null).token());
    var inactive = organizations.invite(manager.actor(), org, true, true, null);
    organizations.membership(owner.actor(), org, manager.id(), false, false, false);
    assertThat(accept(guest.browser(), inactive.token()).toString()).contains("NOT_FOUND");
    organizations.membership(owner.actor(), org, manager.id(), true, true, false);
    assertThat(accept(guest.browser(), inactive.token()).toString()).contains("NOT_FOUND");
    assertThat(access.member(guest.actor(), org)).isFalse();
  }

  @Test
  void membership_concurrentManagerDemotions_preservesLastManager() throws Exception {
    Person a = person(), b = person();
    UUID org = organization(a);
    organizations.accept(b.actor(), organizations.invite(a.actor(), org, true, true, null).token());
    var barrier = new CyclicBarrier(2);
    String query =
        "mutation($org:ID!,$user:ID!){setMembershipPermissions(organizationId:$org,userId:$user,canManage:false,canOrganize:false)}";
    try (var executor = Executors.newFixedThreadPool(2)) {
      var first =
          executor.submit(() -> {
            barrier.await();
            return gql(a.browser(), query, Map.of("org", org, "user", a.id()));
          });
      var second =
          executor.submit(() -> {
            barrier.await();
            return gql(b.browser(), query, Map.of("org", org, "user", b.id()));
          });
      var one = first.get(20, TimeUnit.SECONDS);
      var two = second.get(20, TimeUnit.SECONDS);
      assertThat((one.has("errors") ? 0 : 1) + (two.has("errors") ? 0 : 1)).isEqualTo(1);
    }
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM membership WHERE organization_id=? AND can_manage",
                Integer.class,
                org))
        .isEqualTo(1);
  }

  @Test
  void eventGrant_oneEvent_doesNotGrantOrganizationOrOtherEventManagement() throws Exception {
    Person owner = person(), guest = person();
    UUID org = organization(owner),
        selected = event(owner, org, "Assigned event"),
        other = event(owner, org, "Other event");
    organizations.accept(
        guest.actor(), organizations.invite(owner.actor(), org, false, false, selected).token());
    assertThat(staffing.event(guest.actor(), selected).canManage()).isTrue();
    assertThat(staffing.events(guest.actor(), org, 25, 0).items())
        .extracting(e -> e.id())
        .containsExactly(selected);
    assertThatThrownBy(() -> staffing.event(guest.actor(), other)).hasMessageContaining("access");
    assertThatThrownBy(() -> event(guest, org, "Not authorized")).hasMessageContaining("access");
    assertThatThrownBy(() -> organizations.invite(guest.actor(), org, true, true, null))
        .hasMessageContaining("access");
    var e = staffing.event(guest.actor(), selected);
    assertThat(
            staffing
                .saveEvent(
                    guest.actor(),
                    new EventInput(
                        e.id(),
                        org,
                        "Renamed",
                        "",
                        "",
                        e.eventDate(),
                        "UTC",
                        "PRIVATE",
                        false,
                        e.revision()))
                .title())
        .isEqualTo("Renamed");
    organizations.eventManager(owner.actor(), selected, guest.id(), false);
    assertThat(staffing.event(guest.actor(), selected).canManage()).isFalse();
    assertThatThrownBy(() -> staffing.coverage(guest.actor(), selected, 25, 0))
        .hasMessageContaining("access");
  }

  @Test
  void removeMember_existingRegistration_revokesAccessButPreservesSelfCancellation()
      throws Exception {
    Person owner = person(), guest = person();
    UUID org = organization(owner);
    var membershipInvitation = organizations.invite(owner.actor(), org, false, false, null);
    organizations.accept(guest.actor(), membershipInvitation.token());
    UUID event = event(owner, org, "Staffed event");
    var e = staffing.event(owner.actor(), event);
    var job = staffing.saveJob(owner.actor(), new JobInput(null, event, "Job", "", ""));
    UUID shift =
        staffing
            .saveShift(
                owner.actor(),
                new ShiftInput(
                    null, job.id(), e.eventDate() + "T10:00:00Z", e.eventDate() + "T11:00:00Z", 2))
            .id();
    staffing.saveEvent(
        owner.actor(),
        new EventInput(
            e.id(), org, e.title(), "", "", e.eventDate(), "UTC", "PRIVATE", true, e.revision()));
    var registration = staffing.signup(guest.actor(), shift, guest.id(), UUID.randomUUID(), false);
    tokens.create(guest.actor(), org, "Synthetic scoped token", true);
    organizations.membership(owner.actor(), org, guest.id(), false, false, true);
    assertThat(access.member(guest.actor(), org)).isFalse();
    assertThat(accept(guest.browser(), membershipInvitation.token()).toString())
        .contains("NOT_FOUND");
    assertThat(
            db.queryForObject(
                "SELECT count(*) FROM agent_token WHERE user_id=? AND NOT revoked",
                Integer.class,
                guest.id()))
        .isZero();
    assertThat(staffing.myRegistrations(guest.actor(), 25, 0)).hasSize(1);
    assertThat(staffing.cancel(guest.actor(), registration.id(), false).status())
        .isEqualTo("CANCELLED");
    assertThatThrownBy(
            () -> staffing.signup(guest.actor(), shift, guest.id(), UUID.randomUUID(), false))
        .hasMessageContaining("access");
  }

  @Test
  void organizationPermissions_unrelatedUserAndAgent_cannotEscalateOrCrossScope() throws Exception {
    Person owner = person(), other = person();
    UUID org = organization(owner), otherOrg = organization(other);
    assertThatThrownBy(() -> organizations.invite(other.actor(), org, true, true, null))
        .hasMessageContaining("access");
    assertThatThrownBy(() -> organizations.audit(other.actor(), org, 25, 0))
        .hasMessageContaining("access");
    assertThatThrownBy(() ->
            organizations.create(new Actor(owner.id(), org, true, true), "Agent organization"))
        .hasMessageContaining("access");
    assertThatThrownBy(() ->
            organizations.invite(new Actor(owner.id(), org, true, true), org, true, true, null))
        .hasMessageContaining("access");
    assertThat(access.organizer(new Actor(owner.id(), otherOrg, true, true), org)).isFalse();
    assertThatThrownBy(
            () -> organizations.membership(owner.actor(), org, owner.id(), false, false, true))
        .hasMessageContaining("at least one");
    assertThat(organizations.audit(owner.actor(), org, 25, 0)).isNotEmpty();
    assertThatThrownBy(() -> organizations.invitations(owner.actor(), org, 1000, 0))
        .hasMessageContaining("page size");
  }
}
