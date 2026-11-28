package org.whitesborobandboosters.rallyboro.staffing;

import java.util.List;
import java.util.UUID;

public final class Models {
  private Models() {}

  public record EventInput(
      UUID id,
      UUID organizationId,
      String title,
      String description,
      String location,
      String eventDate,
      String timezone,
      String visibility,
      boolean signupOpen,
      Integer revision) {}

  public record JobInput(
      UUID id, UUID eventId, String name, String description, String eligibility) {}

  public record ShiftInput(UUID id, UUID jobId, String startsAt, String endsAt, int capacity) {}

  public record Viewer(UUID id, String displayName, List<Membership> memberships) {}

  public record Membership(UUID organizationId, String organizationName, String role) {}

  public record Member(UUID id, String displayName, String role) {}

  public record EventPage(List<EventData> items, int total) {}

  public record EventData(
      UUID id,
      UUID organizationId,
      String title,
      String description,
      String location,
      String eventDate,
      String timezone,
      String visibility,
      boolean signupOpen,
      int revision,
      boolean canManage,
      List<JobData> jobs) {}

  public record JobData(
      UUID id,
      UUID eventId,
      String name,
      String description,
      String eligibility,
      List<ShiftData> shifts) {}

  public record ShiftData(
      UUID id,
      UUID jobId,
      String startsAt,
      String endsAt,
      int capacity,
      int filled,
      int remaining) {}

  public record RegistrationData(
      UUID id, String status, String participantName, EventData event, ShiftData shift) {}

  public record AgentToken(
      UUID id,
      String name,
      UUID organizationId,
      boolean writeEnabled,
      String expiresAt,
      boolean revoked) {}

  public record TokenGrant(String token, AgentToken details) {}
}
