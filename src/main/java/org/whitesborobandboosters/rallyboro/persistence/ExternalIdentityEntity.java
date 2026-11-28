package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "external_identity")
public class ExternalIdentityEntity {
  @EmbeddedId private ExternalIdentityId id;

  @Column(name = "user_id")
  private UUID userId;

  protected ExternalIdentityEntity() {}

  public ExternalIdentityEntity(ExternalIdentityId id) {
    this.id = id;
  }

  public ExternalIdentityId getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }
}
