package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "display_name", length = 80)
  private String displayName;

  @Column(name = "auth_version")
  private int authVersion = 0;

  protected UserEntity() {}

  public UserEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public int getAuthVersion() {
    return authVersion;
  }

  public void setAuthVersion(int authVersion) {
    this.authVersion = authVersion;
  }
}
