package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ReceiptId implements Serializable {
  @Column(name = "user_id")
  private UUID userId;

  @Column(name = "request_id")
  private UUID requestId;

  protected ReceiptId() {}

  public ReceiptId(UUID userId, UUID requestId) {
    this.userId = userId;
    this.requestId = requestId;
  }

  public UUID getUserId() {
    return userId;
  }

  public UUID getRequestId() {
    return requestId;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ReceiptId that
        && Objects.equals(userId, that.userId)
        && Objects.equals(requestId, that.requestId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, requestId);
  }
}
