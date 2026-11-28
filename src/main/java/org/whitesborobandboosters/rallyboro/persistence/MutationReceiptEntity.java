package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "mutation_receipt")
public class MutationReceiptEntity {
  @EmbeddedId private ReceiptId id;

  @Column(name = "fingerprint", length = 64)
  private String fingerprint;

  @Column(name = "result_id")
  private UUID resultId;

  protected MutationReceiptEntity() {}

  public MutationReceiptEntity(ReceiptId id) {
    this.id = id;
  }

  public ReceiptId getId() {
    return id;
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public void setFingerprint(String fingerprint) {
    this.fingerprint = fingerprint;
  }

  public UUID getResultId() {
    return resultId;
  }

  public void setResultId(UUID resultId) {
    this.resultId = resultId;
  }
}
