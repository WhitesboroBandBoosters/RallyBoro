package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "event")
public class EventEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "organization_id")
  private UUID organizationId;

  @Column(name = "title", length = 120)
  private String title;

  @Column(name = "description", length = 2000)
  private String description = "";

  @Column(name = "location", length = 200)
  private String location = "";

  @Column(name = "event_date")
  private LocalDate eventDate;

  @Column(name = "timezone", length = 80)
  private String timezone;

  @Column(name = "visibility", length = 10)
  private String visibility = "HIDDEN";

  @Column(name = "signup_open")
  private boolean signupOpen;

  @Column(name = "revision")
  private int revision;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", insertable = false, updatable = false)
  private OrganizationEntity organization;

  protected EventEntity() {}

  public EventEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(UUID organizationId) {
    this.organizationId = organizationId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public LocalDate getEventDate() {
    return eventDate;
  }

  public void setEventDate(LocalDate eventDate) {
    this.eventDate = eventDate;
  }

  public String getTimezone() {
    return timezone;
  }

  public void setTimezone(String timezone) {
    this.timezone = timezone;
  }

  public String getVisibility() {
    return visibility;
  }

  public void setVisibility(String visibility) {
    this.visibility = visibility;
  }

  public boolean isSignupOpen() {
    return signupOpen;
  }

  public void setSignupOpen(boolean signupOpen) {
    this.signupOpen = signupOpen;
  }

  public int getRevision() {
    return revision;
  }

  public void setRevision(int revision) {
    this.revision = revision;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public OrganizationEntity getOrganization() {
    return organization;
  }
}
