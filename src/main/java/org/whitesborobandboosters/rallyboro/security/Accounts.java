package org.whitesborobandboosters.rallyboro.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;

@Service
public class Accounts {
  private final JdbcTemplate db;
  private final PasswordEncoder encoder;
  private final boolean enabled;
  private final int limit;

  private record Window(long minute, int count) {}

  private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

  public record AccountCreated(UUID id, String recoveryKey) {}

  public Accounts(
      JdbcTemplate db,
      PasswordEncoder encoder,
      @Value("${rallyboro.local-accounts:true}") boolean enabled,
      @Value("${rallyboro.account-attempts-per-minute:10}") int limit) {
    this.db = db;
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
    if (db.queryForObject(
            "SELECT count(*) FROM local_credential WHERE username=?", Integer.class, normalized)
        > 0) throw new DomainException("CONFLICT", "This username is unavailable.");
    String passwordHash = hash(password), key = secret();
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO app_user(id,display_name) VALUES (?,?)", id, displayName.strip());
    db.update(
        "INSERT INTO local_credential(user_id,username,password_hash,recovery_hash) VALUES"
            + " (?,?,?,?)",
        id,
        normalized,
        passwordHash,
        SecurityConfig.digest(key));
    db.update("INSERT INTO participant(id,managing_user_id) VALUES (?,?)", id, id);
    return new AccountCreated(id, key);
  }

  @Transactional
  public String recover(String name, String key, String password, String address) {
    throttle(address);
    if (key == null || key.length() > 100)
      throw new DomainException("FORBIDDEN", "Invalid username or recovery key.");
    var rows =
        db.queryForList(
            "SELECT * FROM local_credential WHERE username=? FOR UPDATE", username(name));
    if (rows.isEmpty() || !SecurityConfig.digest(key).equals(rows.getFirst().get("recovery_hash")))
      throw new DomainException("FORBIDDEN", "Invalid username or recovery key.");
    String replacement = secret();
    UUID id = (UUID) rows.getFirst().get("user_id");
    db.update(
        "UPDATE local_credential SET password_hash=?,recovery_hash=?,updated_at=now() WHERE"
            + " user_id=?",
        hash(password),
        SecurityConfig.digest(replacement),
        id);
    invalidate(id);
    return replacement;
  }

  @Transactional
  public boolean changePassword(Actor actor, String current, String password, String address) {
    Access.requireSession(actor);
    throttle(address);
    var rows =
        db.queryForList("SELECT * FROM local_credential WHERE user_id=? FOR UPDATE", actor.id());
    if (rows.isEmpty()
        || current == null
        || current.length() > 100
        || !encoder.matches(current, rows.getFirst().get("password_hash").toString()))
      throw new DomainException("FORBIDDEN", "The current password is incorrect.");
    db.update(
        "UPDATE local_credential SET password_hash=?,updated_at=now() WHERE user_id=?",
        hash(password),
        actor.id());
    invalidate(actor.id());
    return true;
  }

  @Transactional
  public String rotateRecoveryKey(Actor actor, String current, String address) {
    Access.requireSession(actor);
    throttle(address);
    var rows =
        db.queryForList(
            "SELECT password_hash FROM local_credential WHERE user_id=? FOR UPDATE", actor.id());
    if (rows.isEmpty()
        || current == null
        || current.length() > 100
        || !encoder.matches(current, rows.getFirst().get("password_hash").toString()))
      throw new DomainException("FORBIDDEN", "The current password is incorrect.");
    String key = secret();
    db.update(
        "UPDATE local_credential SET recovery_hash=?,updated_at=now() WHERE user_id=?",
        SecurityConfig.digest(key),
        actor.id());
    return key;
  }

  private void invalidate(UUID id) {
    db.update("UPDATE app_user SET auth_version=auth_version+1 WHERE id=?", id);
    db.update("UPDATE agent_token SET revoked=true WHERE user_id=?", id);
  }
}
