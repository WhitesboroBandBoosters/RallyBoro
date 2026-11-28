package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "participant")
public class ParticipantEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "managing_user_id")
  private UUID userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "managing_user_id", insertable = false, updatable = false)
  private UserEntity user;

  protected ParticipantEntity() {}

  public ParticipantEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
  }

  public UserEntity getUser() {
    return user;
  }
}
