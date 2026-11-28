package org.whitesborobandboosters.rallyboro.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.CredentialEntity;
import org.whitesborobandboosters.rallyboro.persistence.ExternalIdentityEntity;
import org.whitesborobandboosters.rallyboro.persistence.ExternalIdentityId;
import org.whitesborobandboosters.rallyboro.persistence.MembershipEntity;
import org.whitesborobandboosters.rallyboro.persistence.MembershipId;
import org.whitesborobandboosters.rallyboro.persistence.OrganizationEntity;
import org.whitesborobandboosters.rallyboro.persistence.ParticipantEntity;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.persistence.PilotImportEntity;
import org.whitesborobandboosters.rallyboro.persistence.UserEntity;

@Component
public class PilotAccounts implements ApplicationRunner, UserDetailsService {
  private final PersistenceRepositories data;
  private final Properties accounts = new Properties();
  private final UUID org;
  private final String orgName;
  private final boolean localEnabled;

  public PilotAccounts(
      PersistenceRepositories data,
      @Value("${rallyboro.accounts-file:}") String file,
      @Value("${rallyboro.local-accounts:true}") boolean localEnabled,
      @Value("${rallyboro.organization-id:00000000-0000-0000-0000-000000000001}") UUID org,
      @Value("${rallyboro.organization-name:RallyBoro Pilot}") String orgName)
      throws IOException {
    this.data = data;
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
    if (!data.organizations().existsById(org)) {
      var organization = new OrganizationEntity(org);
      organization.setName(orgName);
      data.organizations().persist(organization);
    }
    for (String username : users()) {
      if (!username.matches("[a-zA-Z0-9_-]{1,40}"))
        throw new IllegalStateException("Invalid pilot account identifier");
      String identity = accounts.getProperty(username + ".oidc-identity", "local:" + username);
      UUID id = Access.identity(identity);
      if (data.imports().existsById(id)) continue;
      String name = accounts.getProperty(username + ".name");
      String role = accounts.getProperty(username + ".role", "VOLUNTEER");
      if (name.isBlank() || name.length() > 80 || !Set.of("ORGANIZER", "VOLUNTEER").contains(role))
        throw new IllegalStateException("Invalid pilot account configuration");
      if (!data.users().existsById(id)) {
        var user = new UserEntity(id);
        user.setDisplayName(name);
        data.users().persist(user);
      }
      var membershipId = new MembershipId(id, org);
      if (!data.memberships().existsById(membershipId)) {
        var membership = new MembershipEntity(membershipId);
        membership.setRole(role);
        membership.setCanManage(role.equals("ORGANIZER"));
        membership.setCanOrganize(role.equals("ORGANIZER"));
        data.memberships().persist(membership);
      }
      if (accounts.containsKey(username + ".oidc-identity")) {
        int split = identity.lastIndexOf('|');
        if (split < 1) throw new IllegalStateException("Invalid pilot OIDC identity");
        var key =
            new ExternalIdentityId(identity.substring(0, split), identity.substring(split + 1));
        if (!data.identities().existsById(key)) {
          var linked = new ExternalIdentityEntity(key);
          linked.setUserId(id);
          data.identities().persist(linked);
        }
      } else if (accounts.getProperty(username + ".password-hash") != null
          && !data.credentials().existsById(id)) {
        var credential = new CredentialEntity(id);
        credential.setUsername(username.toLowerCase(Locale.ROOT));
        credential.setPasswordHash(accounts.getProperty(username + ".password-hash"));
        data.credentials().persist(credential);
      }
      if (data.participants().findByUserId(id).isEmpty()) {
        var participant = new ParticipantEntity(id);
        participant.setUserId(id);
        data.participants().persist(participant);
      }
      data.imports().persist(new PilotImportEntity(id));
    }
  }

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) {
    if (!localEnabled || username == null || username.length() > 40)
      throw new UsernameNotFoundException("Invalid credentials");
    var credential =
        data.credentials()
            .findByUsername(username.toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    return new AccountUser(
        credential.getId(),
        credential.getUser().getAuthVersion(),
        credential.getUsername(),
        credential.getPasswordHash());
  }
}
