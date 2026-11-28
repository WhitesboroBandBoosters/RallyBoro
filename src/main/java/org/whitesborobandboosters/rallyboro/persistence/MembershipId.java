package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MembershipId implements Serializable {
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "organization_id")
  private UUID organizationId;

  protected MembershipId() {}

  public MembershipId(UUID userId, UUID organizationId) {
    this.userId = userId;
    this.organizationId = organizationId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MembershipId that
        && Objects.equals(userId, that.userId)
        && Objects.equals(organizationId, that.organizationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, organizationId);
  }
}
