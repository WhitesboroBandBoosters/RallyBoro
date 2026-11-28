package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_invitation")
public class InvitationEntity {
  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "organization_id")
  private UUID organizationId;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "token_hash", length = 64)
  private String tokenHash;

  @Column(name = "can_manage")
  private boolean canManage;

  @Column(name = "can_organize")
  private boolean canOrganize;

  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "revoked")
  private boolean revoked;

  @Column(name = "accepted_by")
  private UUID acceptedBy;

  @Column(name = "created_at")
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", insertable = false, updatable = false)
  private OrganizationEntity organization;

  protected InvitationEntity() {}

  public InvitationEntity(UUID id) {
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

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public boolean isCanManage() {
    return canManage;
  }

  public void setCanManage(boolean canManage) {
    this.canManage = canManage;
  }

  public boolean isCanOrganize() {
    return canOrganize;
  }

  public void setCanOrganize(boolean canOrganize) {
    this.canOrganize = canOrganize;
  }

  public UUID getEventId() {
    return eventId;
  }

  public void setEventId(UUID eventId) {
    this.eventId = eventId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }

  public UUID getAcceptedBy() {
    return acceptedBy;
  }

  public void setAcceptedBy(UUID acceptedBy) {
    this.acceptedBy = acceptedBy;
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
