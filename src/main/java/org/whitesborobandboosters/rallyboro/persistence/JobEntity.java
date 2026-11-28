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
@Table(name = "job")
public class JobEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "name", length = 120)
  private String name;

  @Column(name = "description", length = 1000)
  private String description = "";

  @Column(name = "eligibility", length = 1000)
  private String eligibility = "";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "event_id", insertable = false, updatable = false)
  private EventEntity event;

  protected JobEntity() {}

  public JobEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getEligibility() {
    return eligibility;
  }

  public void setEligibility(String eligibility) {
    this.eligibility = eligibility;
  }

  public EventEntity getEvent() {
    return event;
  }
}
