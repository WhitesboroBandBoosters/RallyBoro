package org.whitesborobandboosters.rallyboro.organization;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.whitesborobandboosters.rallyboro.organization.Organizations.AuditEntry;
import org.whitesborobandboosters.rallyboro.organization.Organizations.Invitation;
import org.whitesborobandboosters.rallyboro.organization.Organizations.InvitationGrant;
import org.whitesborobandboosters.rallyboro.organization.Organizations.OrganizationData;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Accounts;
import org.whitesborobandboosters.rallyboro.security.Accounts.AccountCreated;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.Member;

@Controller
public class OrganizationController {
  private final Organizations organizations;
  private final Accounts accounts;
  private final Access access;

  public OrganizationController(Organizations organizations, Accounts accounts, Access access) {
    this.organizations = organizations;
    this.accounts = accounts;
    this.access = access;
  }

  private void signedOut(Principal principal) {
    if (principal != null && !(principal instanceof AnonymousAuthenticationToken))
      throw new DomainException(
          "FORBIDDEN", "Sign out before creating or recovering another account.");
  }

  @MutationMapping
  public AccountCreated registerAccount(
      Principal principal,
      @Argument String username,
      @Argument String displayName,
      @Argument String password,
      @ContextValue("clientAddress") String address) {
    signedOut(principal);
    return accounts.register(username, displayName, password, address);
  }

  @MutationMapping
  public String recoverAccount(
      Principal principal,
      @Argument String username,
      @Argument String recoveryKey,
      @Argument String newPassword,
      @ContextValue("clientAddress") String address) {
    signedOut(principal);
    return accounts.recover(username, recoveryKey, newPassword, address);
  }

  @MutationMapping
  public boolean changePassword(
      Principal principal,
      @Argument String currentPassword,
      @Argument String newPassword,
      @ContextValue("clientAddress") String address) {
    return accounts.changePassword(access.actor(principal), currentPassword, newPassword, address);
  }

  @MutationMapping
  public String rotateRecoveryKey(
      Principal principal,
      @Argument String currentPassword,
      @ContextValue("clientAddress") String address) {
    return accounts.rotateRecoveryKey(access.actor(principal), currentPassword, address);
  }

  @MutationMapping
  public OrganizationData createOrganization(Principal principal, @Argument String name) {
    return organizations.create(access.actor(principal), name);
  }

  @MutationMapping
  public InvitationGrant createInvitation(
      Principal principal,
      @Argument UUID organizationId,
      @Argument boolean canManage,
      @Argument boolean canOrganize,
      @Argument UUID eventId) {
    return organizations.invite(
        access.actor(principal), organizationId, canManage, canOrganize, eventId);
  }

  @QueryMapping
  public Invitation invitation(Principal principal, @Argument String token) {
    return organizations.preview(access.actor(principal), token);
  }

  @QueryMapping
  public List<Invitation> organizationInvitations(
      Principal principal,
      @Argument UUID organizationId,
      @Argument int first,
      @Argument int offset) {
    return organizations.invitations(access.actor(principal), organizationId, first, offset);
  }

  @MutationMapping
  public OrganizationData acceptInvitation(Principal principal, @Argument String token) {
    return organizations.accept(access.actor(principal), token);
  }

  @MutationMapping
  public boolean revokeInvitation(
      Principal principal, @Argument UUID organizationId, @Argument UUID id) {
    return organizations.revoke(access.actor(principal), organizationId, id);
  }

  @MutationMapping
  public boolean setMembershipPermissions(
      Principal principal,
      @Argument UUID organizationId,
      @Argument UUID userId,
      @Argument boolean canManage,
      @Argument boolean canOrganize) {
    return organizations.membership(
        access.actor(principal), organizationId, userId, canManage, canOrganize, false);
  }

  @MutationMapping
  public boolean removeMember(
      Principal principal, @Argument UUID organizationId, @Argument UUID userId) {
    return organizations.membership(
        access.actor(principal), organizationId, userId, false, false, true);
  }

  @MutationMapping
  public boolean setEventManager(
      Principal principal,
      @Argument UUID eventId,
      @Argument UUID userId,
      @Argument boolean enabled) {
    return organizations.eventManager(access.actor(principal), eventId, userId, enabled);
  }

  @QueryMapping
  public List<Member> eventManagers(
      Principal principal, @Argument UUID eventId, @Argument int first, @Argument int offset) {
    return organizations.eventManagers(access.actor(principal), eventId, first, offset);
  }

  @QueryMapping
  public List<AuditEntry> organizationAudit(
      Principal principal,
      @Argument UUID organizationId,
      @Argument int first,
      @Argument int offset) {
    return organizations.audit(access.actor(principal), organizationId, first, offset);
  }
}
