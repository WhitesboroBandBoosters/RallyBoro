package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_manager")
public class EventManagerEntity {
  @EmbeddedId private EventManagerId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", insertable = false, updatable = false)
  private EventEntity event;

  protected EventManagerEntity() {}

  public EventManagerEntity(EventManagerId id) {
    this.id = id;
  }

  public EventManagerId getId() {
    return id;
  }

  public UserEntity getUser() {
    return user;
  }

  public EventEntity getEvent() {
    return event;
  }
}
