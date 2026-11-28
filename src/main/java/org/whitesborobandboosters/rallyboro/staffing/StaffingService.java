package org.whitesborobandboosters.rallyboro.staffing;

import static org.whitesborobandboosters.rallyboro.persistence.StaffingSpecifications.*;
import static org.whitesborobandboosters.rallyboro.staffing.Models.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.persistence.EventEntity;
import org.whitesborobandboosters.rallyboro.persistence.JobEntity;
import org.whitesborobandboosters.rallyboro.persistence.MembershipId;
import org.whitesborobandboosters.rallyboro.persistence.MutationReceiptEntity;
import org.whitesborobandboosters.rallyboro.persistence.OffsetPage;
import org.whitesborobandboosters.rallyboro.persistence.PersistenceRepositories;
import org.whitesborobandboosters.rallyboro.persistence.ReceiptId;
import org.whitesborobandboosters.rallyboro.persistence.RegistrationEntity;
import org.whitesborobandboosters.rallyboro.persistence.ShiftEntity;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.security.SecurityConfig;

@Service
@Transactional(readOnly = true)
public class StaffingService {
  private final PersistenceRepositories data;
  private final Access access;

  public StaffingService(PersistenceRepositories data, Access access) {
    this.data = data;
    this.access = access;
  }

  private static DomainException unavailable() {
    return new DomainException("NOT_FOUND", "This record is unavailable.");
  }

  private static void require(boolean valid, String code, String message) {
    if (!valid) throw new DomainException(code, message);
  }

  private static String text(String value, int max, boolean required) {
    require(
        value != null && value.length() <= max && (!required || !value.isBlank()),
        "INVALID_INPUT",
        "Please check the required fields and length limits.");
    return value.strip();
  }

  private static OffsetPage page(int first, int offset, Sort sort) {
    require(
        first >= 1 && first <= 100 && offset >= 0 && offset <= 10000,
        "INVALID_INPUT",
        "Page size must be 1–100 and offset 0–10000.");
    return new OffsetPage(first, offset, sort);
  }

  private EventEntity eventRow(UUID id) {
    return data.events().findById(id).orElseThrow(StaffingService::unavailable);
  }

  private ShiftEntity shiftRow(UUID id) {
    return data.shifts().findById(id).orElseThrow(StaffingService::unavailable);
  }

  private JobEntity jobRow(UUID id) {
    return data.jobs().findById(id).orElseThrow(StaffingService::unavailable);
  }

  private void visible(Actor actor, EventEntity event) {
    UUID org = event.getOrganizationId();
    if (actor.organizationId() != null && !actor.organizationId().equals(org)) Access.denied();
    if (!access.manageEvent(actor, event.getId(), org)
        && !data.registrations()
            .existsByParticipantUserIdAndShiftJobEventId(actor.id(), event.getId())) {
      if (event.getVisibility().equals("HIDDEN")
          || (event.getVisibility().equals("PRIVATE") && !access.member(actor, org)))
        Access.denied();
    }
  }

  public Viewer viewer(Actor actor) {
    var user = data.users().findById(actor.id()).orElseThrow(StaffingService::unavailable);
    var memberships =
        data.memberships().findByIdUserId(actor.id()).stream()
            .filter(m ->
                actor.organizationId() == null
                    || actor.organizationId().equals(m.getId().getOrganizationId()))
            .map(m ->
                new Membership(
                    m.getId().getOrganizationId(),
                    m.getOrganization().getName(),
                    m.getRole(),
                    m.isCanManage(),
                    m.isCanManage() || m.isCanOrganize()))
            .toList();
    return new Viewer(user.getId(), user.getDisplayName(), memberships);
  }

  public EventPage events(Actor actor, UUID org, int first, int offset) {
    var page = page(first, offset, Sort.by("eventDate", "id"));
    if (!access.member(actor, org)) Access.denied();
    var filter = visibleEvents(org, actor.id(), access.organizer(actor, org));
    var result = data.events().findAll(filter, page);
    return new EventPage(
        result.stream().map(e -> eventData(actor, e)).toList(),
        Math.toIntExact(result.getTotalElements()));
  }

  public EventData event(Actor actor, UUID id) {
    var event = eventRow(id);
    visible(actor, event);
    return eventData(actor, event);
  }

  public List<SignupGuidance> signupGuidance(Actor actor, UUID eventId) {
    visible(actor, eventRow(eventId));
    var shifts = data.shifts().findByJobEventIdOrderByStartsAtAscIdAsc(eventId);
    if (shifts.isEmpty()) return List.of();
    Instant first = shifts.getFirst().getStartsAt(),
        last = shifts.stream().map(ShiftEntity::getEndsAt).max(Instant::compareTo).orElseThrow();
    var own =
        data.registrations()
            .findByParticipantUserIdAndStatusAndShiftStartsAtLessThanAndShiftEndsAtGreaterThan(
                actor.id(), "CONFIRMED", last, first);
    return shifts.stream()
        .map(shift -> {
          UUID registered =
              own.stream()
                  .filter(r -> r.getShiftId().equals(shift.getId()))
                  .map(RegistrationEntity::getId)
                  .findFirst()
                  .orElse(null);
          boolean conflict =
              own.stream()
                  .anyMatch(r ->
                      !r.getShiftId().equals(shift.getId())
                          && r.getShift().getStartsAt().isBefore(shift.getEndsAt())
                          && r.getShift().getEndsAt().isAfter(shift.getStartsAt()));
          return new SignupGuidance(shift.getId(), registered, conflict);
        })
        .toList();
  }

  private EventData eventData(Actor actor, EventEntity event) {
    return new EventData(
        event.getId(),
        event.getOrganizationId(),
        event.getTitle(),
        event.getDescription(),
        event.getLocation(),
        event.getEventDate().toString(),
        event.getTimezone(),
        event.getVisibility(),
        event.isSignupOpen(),
        event.getRevision(),
        access.manageEvent(actor, event.getId(), event.getOrganizationId()),
        data.jobs().findByEventIdOrderById(event.getId()).stream().map(this::jobData).toList());
  }

  private JobData jobData(JobEntity job) {
    return new JobData(
        job.getId(),
        job.getEventId(),
        job.getName(),
        job.getDescription(),
        job.getEligibility(),
        data.shifts().findByJobIdOrderByStartsAtAscIdAsc(job.getId()).stream()
            .map(this::shiftData)
            .toList());
  }

  private ShiftData shiftData(ShiftEntity shift) {
    int filled =
        Math.toIntExact(data.registrations().countByShiftIdAndStatus(shift.getId(), "CONFIRMED"));
    return new ShiftData(
        shift.getId(),
        shift.getJobId(),
        shift.getStartsAt().toString(),
        shift.getEndsAt().toString(),
        shift.getCapacity(),
        filled,
        shift.getCapacity() - filled);
  }

  private EventEntity lockEvent(UUID id) {
    return data.events().lockById(id).orElseThrow(StaffingService::unavailable);
  }

  private EventEntity editable(Actor actor, UUID id) {
    var event = lockEvent(id);
    access.requireEvent(actor, id, event.getOrganizationId());
    access.write(actor);
    return event;
  }

  private void noAssignments(UUID event) {
    require(
        !data.registrations().existsByShiftJobEventIdAndStatus(event, "CONFIRMED"),
        "CONFLICT",
        "Remove confirmed assignments before changing event timing or staffing structure.");
  }

  @Transactional
  public EventData saveEvent(Actor actor, EventInput input) {
    access.write(actor);
    if (input.id() == null) access.requireOrganizer(actor, input.organizationId());
    else access.requireEvent(actor, input.id(), input.organizationId());
    String title = text(input.title(), 120, true),
        description = text(input.description(), 2000, false),
        location = text(input.location(), 200, false);
    LocalDate date = LocalDate.parse(input.eventDate());
    ZoneId.of(input.timezone());
    require(
        List.of("HIDDEN", "PRIVATE", "PUBLIC").contains(input.visibility()),
        "INVALID_INPUT",
        "Select an event visibility.");
    require(
        !input.signupOpen() || !input.visibility().equals("HIDDEN"),
        "INVALID_INPUT",
        "Make the event visible before opening signup.");
    EventEntity event;
    if (input.id() == null) {
      require(!input.signupOpen(), "INVALID_INPUT", "New events start with signup closed.");
      event = new EventEntity(UUID.randomUUID());
      event.setOrganizationId(input.organizationId());
    } else {
      event = editable(actor, input.id());
      require(
          event.getOrganizationId().equals(input.organizationId()),
          "INVALID_INPUT",
          "An event cannot change organizations.");
      require(
          input.revision() != null && input.revision() == event.getRevision(),
          "CONFLICT",
          "This event changed. Reload before saving.");
      if (!event.getEventDate().equals(date) || !event.getTimezone().equals(input.timezone())) {
        noAssignments(event.getId());
        require(
            data.shifts().countByJobEventId(event.getId()) == 0,
            "CONFLICT",
            "Event date and timezone are fixed after shifts are added. Create a new event to"
                + " reschedule.");
      }
      event.setRevision(event.getRevision() + 1);
    }
    event.setTitle(title);
    event.setDescription(description);
    event.setLocation(location);
    event.setEventDate(date);
    event.setTimezone(input.timezone());
    event.setVisibility(input.visibility());
    event.setSignupOpen(input.signupOpen());
    if (input.id() == null) data.events().persist(event);
    return eventData(actor, event);
  }

  @Transactional
  public JobData saveJob(Actor actor, JobInput input) {
    editable(actor, input.eventId());
    noAssignments(input.eventId());
    String name = text(input.name(), 120, true),
        description = text(input.description(), 1000, false),
        eligibility = text(input.eligibility(), 1000, false);
    JobEntity job;
    if (input.id() == null) {
      require(
          data.jobs().countByEventId(input.eventId()) < 20,
          "LIMIT_EXCEEDED",
          "Pilot events support at most 20 jobs.");
      job = new JobEntity(UUID.randomUUID());
      job.setEventId(input.eventId());
    } else {
      job = jobRow(input.id());
      require(job.getEventId().equals(input.eventId()), "NOT_FOUND", "Job unavailable.");
    }
    job.setName(name);
    job.setDescription(description);
    job.setEligibility(eligibility);
    if (input.id() == null) data.jobs().persist(job);
    return jobData(job);
  }

  @Transactional
  public ShiftData saveShift(Actor actor, ShiftInput input) {
    UUID eventId = jobRow(input.jobId()).getEventId();
    var event = editable(actor, eventId);
    noAssignments(eventId);
    Instant start = Instant.parse(input.startsAt()), end = Instant.parse(input.endsAt());
    require(
        end.isAfter(start) && !end.isAfter(start.plusSeconds(86400)),
        "INVALID_INPUT",
        "A shift must last more than zero and at most 24 hours.");
    require(
        start.atZone(ZoneId.of(event.getTimezone())).toLocalDate().equals(event.getEventDate()),
        "INVALID_INPUT",
        "Shift start must be on the event date in its timezone.");
    require(
        input.capacity() >= 1 && input.capacity() <= 1000,
        "INVALID_INPUT",
        "Capacity must be 1–1000.");
    ShiftEntity shift;
    if (input.id() == null) {
      require(
          data.shifts().countByJobId(input.jobId()) < 100,
          "LIMIT_EXCEEDED",
          "Pilot jobs support at most 100 shifts.");
      shift = new ShiftEntity(UUID.randomUUID());
      shift.setJobId(input.jobId());
    } else {
      shift = shiftRow(input.id());
      require(shift.getJobId().equals(input.jobId()), "NOT_FOUND", "Shift unavailable.");
    }
    shift.setStartsAt(start);
    shift.setEndsAt(end);
    shift.setCapacity(input.capacity());
    if (input.id() == null) data.shifts().persist(shift);
    return shiftData(shift);
  }

  private RegistrationData registration(Actor actor, RegistrationEntity registration) {
    var shift = registration.getShift();
    var event = shift.getJob().getEvent();
    var participant = registration.getParticipant();
    if (!participant.getUserId().equals(actor.id()))
      access.requireEvent(actor, event.getId(), event.getOrganizationId());
    if (actor.organizationId() != null && !actor.organizationId().equals(event.getOrganizationId()))
      Access.denied();
    return new RegistrationData(
        registration.getId(),
        registration.getStatus(),
        participant.getUser().getDisplayName(),
        eventData(actor, event),
        shiftData(shift));
  }

  private RegistrationData registration(Actor actor, UUID id) {
    return registration(
        actor, data.registrations().findById(id).orElseThrow(StaffingService::unavailable));
  }

  public List<RegistrationData> myRegistrations(Actor actor, int first, int offset) {
    return data
        .registrations()
        .findAll(
            ownRegistrations(actor.id(), actor.organizationId()),
            page(first, offset, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))))
        .stream()
        .map(r -> registration(actor, r))
        .toList();
  }

  public List<RegistrationData> coverage(Actor actor, UUID eventId, int first, int offset) {
    var page = page(first, offset, Sort.by("createdAt", "id"));
    access.requireEvent(actor, eventId, eventRow(eventId).getOrganizationId());
    return data.registrations().findByShiftJobEventIdAndStatus(eventId, "CONFIRMED", page).stream()
        .map(r -> registration(actor, r))
        .toList();
  }

  public List<Member> members(Actor actor, UUID org, int first, int offset) {
    var page = page(first, offset, Sort.by("user.displayName", "id.userId"));
    if (!access.memberList(actor, org)) Access.denied();
    return data.memberships().findByIdOrganizationId(org, page).stream()
        .map(m ->
            new Member(
                m.getId().getUserId(),
                m.getUser().getDisplayName(),
                m.getRole(),
                m.isCanManage(),
                m.isCanManage() || m.isCanOrganize()))
        .toList();
  }

  @Transactional
  public RegistrationData signup(
      Actor actor, UUID shiftId, UUID targetUser, UUID request, boolean assignment) {
    access.write(actor);
    var shift = shiftRow(shiftId);
    UUID eventId = shift.getJob().getEventId();
    var event = lockEvent(eventId);
    UUID org = event.getOrganizationId();
    if (assignment) access.requireEvent(actor, eventId, org);
    else if (!access.member(actor, org)) Access.denied();
    require(
        data.memberships().existsById(new MembershipId(targetUser, org)),
        "FORBIDDEN",
        "Participant is not a member of this organization.");
    var participant =
        data.participants().findByUserId(targetUser).orElseThrow(StaffingService::unavailable);
    participant =
        data.participants().lockById(participant.getId()).orElseThrow(StaffingService::unavailable);
    String fingerprint =
        SecurityConfig.digest((assignment ? "assign" : "signup") + shiftId + targetUser);
    var receiptId = new ReceiptId(actor.id(), request);
    var receipt = data.receipts().findById(receiptId);
    if (receipt.isPresent()) {
      require(
          receipt.get().getFingerprint().equals(fingerprint),
          "CONFLICT",
          "This request ID was already used for another action.");
      return registration(actor, receipt.get().getResultId());
    }
    require(
        event.isSignupOpen() && !event.getVisibility().equals("HIDDEN"),
        "CLOSED",
        "Signup is closed for this event.");
    shift = data.shifts().lockById(shiftId).orElseThrow(StaffingService::unavailable);
    require(shift.getEndsAt().isAfter(Instant.now()), "CLOSED", "This shift has ended.");
    var previous =
        data.registrations()
            .findByParticipantIdAndShiftIdAndStatus(participant.getId(), shiftId, "CONFIRMED");
    RegistrationEntity registration;
    if (previous.isPresent()) registration = previous.get();
    else {
      require(
          data.registrations().countByShiftIdAndStatus(shiftId, "CONFIRMED") < shift.getCapacity(),
          "FULL",
          "This shift is full. Please choose another shift.");
      require(
          !data.registrations()
              .existsByParticipantIdAndStatusAndShiftStartsAtLessThanAndShiftEndsAtGreaterThan(
                  participant.getId(), "CONFIRMED", shift.getEndsAt(), shift.getStartsAt()),
          "OVERLAP",
          assignment
              ? "This participant already has a shift at that time."
              : "You already have a shift at this time. View My registrations to review your"
                  + " bookings.");
      registration = new RegistrationEntity(UUID.randomUUID());
      registration.setParticipantId(participant.getId());
      registration.setShiftId(shiftId);
      registration.setStatus("CONFIRMED");
      data.registrations().persist(registration);
    }
    var savedReceipt = new MutationReceiptEntity(receiptId);
    savedReceipt.setFingerprint(fingerprint);
    savedReceipt.setResultId(registration.getId());
    data.receipts().persist(savedReceipt);
    // New entities use scalar FK setters; retrieve navigations only after the insert is visible.
    data.registrations().flush();
    return new RegistrationData(
        registration.getId(),
        registration.getStatus(),
        data.users()
            .findById(targetUser)
            .orElseThrow(StaffingService::unavailable)
            .getDisplayName(),
        eventData(actor, event),
        shiftData(shift));
  }

  @Transactional
  public RegistrationData cancel(Actor actor, UUID id, boolean manager) {
    access.write(actor);
    var registration = data.registrations().findById(id).orElseThrow(StaffingService::unavailable);
    var participant = registration.getParticipant();
    var event = registration.getShift().getJob().getEvent();
    lockEvent(event.getId());
    if (manager) access.requireEvent(actor, event.getId(), event.getOrganizationId());
    else if (!participant.getUserId().equals(actor.id())) Access.denied();
    if (actor.organizationId() != null && !actor.organizationId().equals(event.getOrganizationId()))
      Access.denied();
    data.participants().lockById(participant.getId()).orElseThrow(StaffingService::unavailable);
    registration = data.registrations().lockById(id).orElseThrow(StaffingService::unavailable);
    registration.setStatus("CANCELLED");
    registration.setUpdatedAt(Instant.now());
    return registration(actor, registration);
  }
}
