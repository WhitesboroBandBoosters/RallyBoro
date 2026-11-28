package org.whitesborobandboosters.rallyboro.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PilotAccounts implements ApplicationRunner, UserDetailsService {
  private final JdbcTemplate jdbc;
  private final Properties accounts = new Properties();
  private final UUID org;
  private final String orgName;
  private final boolean localEnabled;

  public PilotAccounts(
      JdbcTemplate jdbc,
      @Value("${rallyboro.accounts-file:}") String file,
      @Value("${rallyboro.pilot-login:false}") boolean localEnabled,
      @Value("${rallyboro.organization-id:00000000-0000-0000-0000-000000000001}") UUID org,
      @Value("${rallyboro.organization-name:RallyBoro Pilot}") String orgName)
      throws IOException {
    this.jdbc = jdbc;
    this.org = org;
    this.orgName = orgName;
    this.localEnabled = localEnabled;
    if (!file.isBlank())
      try (InputStream input = Files.newInputStream(Path.of(file))) {
        accounts.load(input);
      }
  }

  private Set<String> users() {
    return accounts.stringPropertyNames().stream()
        .filter(k -> k.endsWith(".name"))
        .map(k -> k.substring(0, k.length() - 5))
        .collect(Collectors.toSet());
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (users().isEmpty()) return;
    jdbc.update(
        "INSERT INTO organization(id,name) VALUES (?,?) ON CONFLICT(id) DO NOTHING", org, orgName);
    for (String username : users()) {
      if (!username.matches("[a-zA-Z0-9_-]{1,40}"))
        throw new IllegalStateException("Invalid pilot account identifier");
      String identity = accounts.getProperty(username + ".oidc-identity", "local:" + username);
      UUID id = Access.identity(identity);
      String name = accounts.getProperty(username + ".name");
      String role = accounts.getProperty(username + ".role", "VOLUNTEER");
      if (name.isBlank() || name.length() > 80 || !Set.of("ORGANIZER", "VOLUNTEER").contains(role))
        throw new IllegalStateException("Invalid pilot account configuration");
      jdbc.update(
          "INSERT INTO app_user(id,display_name) VALUES (?,?) ON CONFLICT(id) DO UPDATE SET"
              + " display_name=excluded.display_name",
          id,
          name);
      jdbc.update(
          "INSERT INTO membership(user_id,organization_id,role) VALUES (?,?,?) ON"
              + " CONFLICT(user_id,organization_id) DO UPDATE SET role=excluded.role",
          id,
          org,
          role);
      jdbc.update(
          "INSERT INTO participant(id,managing_user_id) VALUES (?,?) ON CONFLICT(managing_user_id)"
              + " DO NOTHING",
          id,
          id);
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) {
    String hash = accounts.getProperty(username + ".password-hash");
    if (!localEnabled
        || hash == null
        || !users().contains(username)
        || accounts.containsKey(username + ".oidc-identity"))
      throw new UsernameNotFoundException("Invalid credentials");
    return User.withUsername(username).password(hash).roles("PILOT").build();
  }
}
