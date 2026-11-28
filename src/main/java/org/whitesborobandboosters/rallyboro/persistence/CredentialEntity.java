package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "local_credential")
public class CredentialEntity {
  @Id
  @Column(name = "user_id")
  private UUID id;

  @Column(name = "username", length = 40)
  private String username;

  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  @Column(name = "recovery_hash", length = 64)
  private String recoveryHash;

  @Column(name = "updated_at")
  private Instant updatedAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;

  protected CredentialEntity() {}

  public CredentialEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getRecoveryHash() {
    return recoveryHash;
  }

  public void setRecoveryHash(String recoveryHash) {
    this.recoveryHash = recoveryHash;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public UserEntity getUser() {
    return user;
  }
}
