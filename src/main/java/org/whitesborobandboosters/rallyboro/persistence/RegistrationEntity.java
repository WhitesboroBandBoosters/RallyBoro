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
@Table(name = "registration")
public class RegistrationEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "participant_id")
  private UUID participantId;

  @Column(name = "shift_id")
  private UUID shiftId;

  @Column(name = "status", length = 12)
  private String status;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at")
  private Instant updatedAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "participant_id", insertable = false, updatable = false)
  private ParticipantEntity participant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shift_id", insertable = false, updatable = false)
  private ShiftEntity shift;

  protected RegistrationEntity() {}

  public RegistrationEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getParticipantId() {
    return participantId;
  }

  public void setParticipantId(UUID participantId) {
    this.participantId = participantId;
  }

  public UUID getShiftId() {
    return shiftId;
  }

  public void setShiftId(UUID shiftId) {
    this.shiftId = shiftId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public ParticipantEntity getParticipant() {
    return participant;
  }

  public ShiftEntity getShift() {
    return shift;
  }
}
