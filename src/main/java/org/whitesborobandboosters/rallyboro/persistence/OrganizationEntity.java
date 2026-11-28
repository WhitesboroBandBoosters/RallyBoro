package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class OrganizationEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "name", length = 120)
  private String name;

  protected OrganizationEntity() {}

  public OrganizationEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
