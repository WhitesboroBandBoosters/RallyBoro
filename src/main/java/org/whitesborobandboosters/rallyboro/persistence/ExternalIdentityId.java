package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ExternalIdentityId implements Serializable {
  @Column(name = "issuer", length = 1000)
  private String issuer;

  @Column(name = "subject", length = 255)
  private String subject;

  protected ExternalIdentityId() {}

  public ExternalIdentityId(String issuer, String subject) {
    this.issuer = issuer;
    this.subject = subject;
  }

  public String getIssuer() {
    return issuer;
  }

  public String getSubject() {
    return subject;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof ExternalIdentityId that
        && Objects.equals(issuer, that.issuer)
        && Objects.equals(subject, that.subject);
  }

  @Override
  public int hashCode() {
    return Objects.hash(issuer, subject);
  }
}
