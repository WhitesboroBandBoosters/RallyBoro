package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "foundation_probe")
public class ProbeEntity {
  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "description")
  private String description;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  protected ProbeEntity() {}

  public ProbeEntity(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
