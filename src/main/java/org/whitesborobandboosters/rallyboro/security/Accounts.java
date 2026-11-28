package org.whitesborobandboosters.rallyboro.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.CredentialEntity;
import org.whitesborobandboosters.rallyboro.persistence.ParticipantEntity;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.persistence.UserEntity;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;

@Service
public class Accounts {
  private final PersistenceRepositories data;
  private final PasswordEncoder encoder;
  private final boolean enabled;
  private final int limit;

  private record Window(long minute, int count) {}

  private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

  public record AccountCreated(UUID id, String recoveryKey) {}

  public Accounts(
      PersistenceRepositories data,
      PasswordEncoder encoder,
      @Value("${rallyboro.local-accounts:true}") boolean enabled,
      @Value("${rallyboro.account-attempts-per-minute:10}") int limit) {
    this.data = data;
    this.encoder = encoder;
    this.enabled = enabled;
    this.limit = limit;
  }

  public static String secret() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private void throttle(String address) {
    if (!enabled)
      throw new DomainException("FORBIDDEN", "Local account creation and recovery are disabled.");
    long minute = Instant.now().getEpochSecond() / 60;
    if (attempts.size() > 10000) attempts.entrySet().removeIf(e -> e.getValue().minute() < minute);
    if (attempts.size() > 10000)
      throw new DomainException("LIMIT_EXCEEDED", "Please try again later.");
    var window =
        attempts.compute(
            address,
            (k, v) -> new Window(minute, v == null || v.minute() != minute ? 1 : v.count() + 1));
    if (window.count() > limit)
      throw new DomainException(
          "LIMIT_EXCEEDED", "Too many account requests. Please try again later.");
  }

  public static String username(String value) {
    if (value == null || !value.matches("[a-zA-Z0-9_-]{3,40}"))
      throw new DomainException(
          "INVALID_INPUT", "Username must be 3–40 letters, digits, underscores, or hyphens.");
    return value.toLowerCase(Locale.ROOT);
  }

  private String hash(String password) {
    int bytes = password == null ? 0 : password.getBytes(StandardCharsets.UTF_8).length;
    if (password == null || password.codePointCount(0, password.length()) < 15 || bytes > 72)
      throw new DomainException(
          "INVALID_INPUT", "Use a password of at least 15 characters and at most 72 UTF-8 bytes.");
    return encoder.encode(password);
  }

  @Transactional
  public AccountCreated register(String name, String displayName, String password, String address) {
    throttle(address);
    String normalized = username(name);
    if (displayName == null || displayName.isBlank() || displayName.length() > 80)
      throw new DomainException("INVALID_INPUT", "Display name must be 1–80 characters.");
    if (data.credentials().existsByUsername(normalized))
      throw new DomainException("CONFLICT", "This username is unavailable.");
    String passwordHash = hash(password), key = secret();
    UUID id = UUID.randomUUID();
    var user = new UserEntity(id);
    user.setDisplayName(displayName.strip());
    data.users().persist(user);
    var credential = new CredentialEntity(id);
    credential.setUsername(normalized);
    credential.setPasswordHash(passwordHash);
    credential.setRecoveryHash(SecurityConfig.digest(key));
    data.credentials().persist(credential);
    var participant = new ParticipantEntity(id);
    participant.setUserId(id);
    data.participants().persist(participant);
    return new AccountCreated(id, key);
  }

  @Transactional
  public String recover(String name, String key, String password, String address) {
    throttle(address);
    if (key == null || key.length() > 100)
      throw new DomainException("FORBIDDEN", "Invalid username or recovery key.");
    var found = data.credentials().findByUsername(username(name));
    var credential =
        found.isEmpty() ? null : data.credentials().lockById(found.get().getId()).orElse(null);
    if (credential == null || !SecurityConfig.digest(key).equals(credential.getRecoveryHash()))
      throw new DomainException("FORBIDDEN", "Invalid username or recovery key.");
    String replacement = secret();
    credential.setPasswordHash(hash(password));
    credential.setRecoveryHash(SecurityConfig.digest(replacement));
    credential.setUpdatedAt(Instant.now());
    invalidate(credential.getId());
    return replacement;
  }

  @Transactional
  public boolean changePassword(Actor actor, String current, String password, String address) {
    Access.requireSession(actor);
    throttle(address);
    var credential = verifiedCredential(actor, current);
    credential.setPasswordHash(hash(password));
    credential.setUpdatedAt(Instant.now());
    invalidate(actor.id());
    return true;
  }

  @Transactional
  public String rotateRecoveryKey(Actor actor, String current, String address) {
    Access.requireSession(actor);
    throttle(address);
    var credential = verifiedCredential(actor, current);
    String key = secret();
    credential.setRecoveryHash(SecurityConfig.digest(key));
    credential.setUpdatedAt(Instant.now());
    return key;
  }

  private CredentialEntity verifiedCredential(Actor actor, String current) {
    var credential = data.credentials().lockById(actor.id()).orElse(null);
    if (credential == null
        || current == null
        || current.length() > 100
        || !encoder.matches(current, credential.getPasswordHash()))
      throw new DomainException("FORBIDDEN", "The current password is incorrect.");
    return credential;
  }

  private void invalidate(UUID id) {
    var user = data.users().lockById(id).orElseThrow();
    user.setAuthVersion(user.getAuthVersion() + 1);
    data.tokens().findByUserId(id).forEach(token -> token.setRevoked(true));
  }
}
