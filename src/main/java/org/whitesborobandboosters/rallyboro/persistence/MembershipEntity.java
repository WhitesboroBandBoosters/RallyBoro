package org.whitesborobandboosters.rallyboro.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "membership")
public class MembershipEntity {
  @EmbeddedId private MembershipId id;

  @Column(name = "role", length = 12)
  private String role;

  @Column(name = "can_manage")
  private boolean canManage;

  @Column(name = "can_organize")
  private boolean canOrganize;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", insertable = false, updatable = false)
  private UserEntity user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "organization_id", insertable = false, updatable = false)
  private OrganizationEntity organization;

  protected MembershipEntity() {}

  public MembershipEntity(MembershipId id) {
    this.id = id;
  }

  public MembershipId getId() {
    return id;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
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

  public UserEntity getUser() {
    return user;
  }

  public OrganizationEntity getOrganization() {
    return organization;
  }
}
