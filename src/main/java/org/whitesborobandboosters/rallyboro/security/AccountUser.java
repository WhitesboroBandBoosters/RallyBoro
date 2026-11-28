package org.whitesborobandboosters.rallyboro.security;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.userdetails.User;

/** Password-authenticated identity; credentials never determine the domain user ID. */
public final class AccountUser extends User {
  private final UUID id;
  private final int version;

  public AccountUser(UUID id, int version, String username, String hash) {
    super(username, hash, List.of());
    this.id = id;
    this.version = version;
  }

  public UUID id() {
    return id;
  }

  public int version() {
    return version;
  }
}
