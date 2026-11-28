package org.whitesborobandboosters.rallyboro.persistence;

import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class StaffingSpecifications {
  private StaffingSpecifications() {}

  public static Specification<EventEntity> visibleEvents(
      UUID organization, UUID user, boolean allEvents) {
    return (event, query, cb) -> {
      var inOrganization = cb.equal(event.get(EventEntity_.organizationId), organization);
      if (allEvents) return inOrganization;
      var grants = query.subquery(Integer.class);
      var grant = grants.from(EventManagerEntity.class);
      grants
          .select(cb.literal(1))
          .where(
              cb.equal(
                  grant.get(EventManagerEntity_.id).get(EventManagerId_.eventId),
                  event.get(EventEntity_.id)),
              cb.equal(grant.get(EventManagerEntity_.id).get(EventManagerId_.userId), user));
      return cb.and(
          inOrganization,
          cb.or(cb.notEqual(event.get(EventEntity_.visibility), "HIDDEN"), cb.exists(grants)));
    };
  }

  public static Specification<RegistrationEntity> ownRegistrations(UUID user, UUID organization) {
    return (registration, query, cb) -> {
      var own =
          cb.equal(
              registration.get(RegistrationEntity_.participant).get(ParticipantEntity_.userId),
              user);
      return organization == null
          ? own
          : cb.and(
              own,
              cb.equal(
                  registration
                      .get(RegistrationEntity_.shift)
                      .get(ShiftEntity_.job)
                      .get(JobEntity_.event)
                      .get(EventEntity_.organizationId),
                  organization));
    };
  }
}
