package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class EventManagerId implements Serializable {
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "user_id")
  private UUID userId;

  protected EventManagerId() {}

  public EventManagerId(UUID eventId, UUID userId) {
    this.eventId = eventId;
    this.userId = userId;
  }

  public UUID getEventId() {
    return eventId;
  }

  public UUID getUserId() {
    return userId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof EventManagerId that
        && Objects.equals(eventId, that.eventId)
        && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(eventId, userId);
  }
}
