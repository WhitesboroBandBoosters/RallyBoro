package org.whitesborobandboosters.rallyboro.staffing;

import static org.whitesborobandboosters.rallyboro.staffing.Models.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.AgentTokens;

@Controller
public class StaffingController {
  private final StaffingService service;
  private final Access access;
  private final AgentTokens tokens;

  public StaffingController(StaffingService service, Access access, AgentTokens tokens) {
    this.service = service;
    this.access = access;
    this.tokens = tokens;
  }

  @QueryMapping
  public Map<String, String> appInfo() {
    return Map.of("name", "RallyBoro", "stage", "staffing-pilot");
  }

  @QueryMapping
  public Viewer viewer(Principal principal) {
    return service.viewer(access.actor(principal));
  }

  @QueryMapping
  public EventPage events(
      Principal principal,
      @Argument UUID organizationId,
      @Argument int first,
      @Argument int offset) {
    return service.events(access.actor(principal), organizationId, first, offset);
  }

  @QueryMapping
  public EventData event(Principal principal, @Argument UUID id) {
    return service.event(access.actor(principal), id);
  }

  @QueryMapping
  public List<RegistrationData> myRegistrations(
      Principal principal, @Argument int first, @Argument int offset) {
    return service.myRegistrations(access.actor(principal), first, offset);
  }

  @QueryMapping
  public List<RegistrationData> coverage(
      Principal principal, @Argument UUID eventId, @Argument int first, @Argument int offset) {
    return service.coverage(access.actor(principal), eventId, first, offset);
  }

  @QueryMapping
  public List<Member> members(
      Principal principal,
      @Argument UUID organizationId,
      @Argument int first,
      @Argument int offset) {
    return service.members(access.actor(principal), organizationId, first, offset);
  }

  @QueryMapping
  public List<AgentToken> agentTokens(Principal principal) {
    return tokens.list(access.actor(principal));
  }

  @MutationMapping
  public EventData saveEvent(Principal principal, @Argument EventInput input) {
    return service.saveEvent(access.actor(principal), input);
  }

  @MutationMapping
  public JobData saveJob(Principal principal, @Argument JobInput input) {
    return service.saveJob(access.actor(principal), input);
  }

  @MutationMapping
  public ShiftData saveShift(Principal principal, @Argument ShiftInput input) {
    return service.saveShift(access.actor(principal), input);
  }

  @MutationMapping
  public RegistrationData signup(
      Principal principal, @Argument UUID shiftId, @Argument UUID requestId) {
    var actor = access.actor(principal);
    return service.signup(actor, shiftId, actor.id(), requestId, false);
  }

  @MutationMapping
  public RegistrationData assignVolunteer(
      Principal principal,
      @Argument UUID shiftId,
      @Argument UUID userId,
      @Argument UUID requestId) {
    return service.signup(access.actor(principal), shiftId, userId, requestId, true);
  }

  @MutationMapping
  public RegistrationData cancelRegistration(Principal principal, @Argument UUID id) {
    return service.cancel(access.actor(principal), id, false);
  }

  @MutationMapping
  public RegistrationData removeAssignment(Principal principal, @Argument UUID id) {
    return service.cancel(access.actor(principal), id, true);
  }

  @MutationMapping
  public TokenGrant createAgentToken(
      Principal principal,
      @Argument UUID organizationId,
      @Argument String name,
      @Argument boolean writeEnabled) {
    return tokens.create(access.actor(principal), organizationId, name, writeEnabled);
  }

  @MutationMapping
  public boolean revokeAgentToken(Principal principal, @Argument UUID id) {
    return tokens.revoke(access.actor(principal), id);
  }
}
