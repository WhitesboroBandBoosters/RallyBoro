package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "pilot_import")
public class PilotImportEntity {
  @Id
  @Column(name = "user_id")
  private UUID id;

  protected PilotImportEntity() {}

  public PilotImportEntity(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }
}
