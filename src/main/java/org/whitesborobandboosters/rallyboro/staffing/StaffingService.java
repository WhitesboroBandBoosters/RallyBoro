package org.whitesborobandboosters.rallyboro.staffing;

import static org.whitesborobandboosters.rallyboro.staffing.Models.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.security.SecurityConfig;

@Service
@Transactional(readOnly = true)
public class StaffingService {
  private final JdbcTemplate db;
  private final Access access;

  public StaffingService(JdbcTemplate db, Access access) {
    this.db = db;
    this.access = access;
  }

  private static UUID uuid(Map<String, Object> row, String name) {
    return (UUID) row.get(name);
  }

  private static Instant instant(Object value) {
    return value instanceof OffsetDateTime date
        ? date.toInstant()
        : ((Timestamp) value).toInstant();
  }

  private static String str(Map<String, Object> row, String name) {
    return row.get(name).toString();
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

  private static int page(int first, int offset) {
    require(
        first >= 1 && first <= 100 && offset >= 0 && offset <= 10000,
        "INVALID_INPUT",
        "Page size must be 1–100 and offset 0–10000.");
    return first;
  }

  private Map<String, Object> one(String sql, Object... args) {
    var rows = db.queryForList(sql, args);
    if (rows.isEmpty()) throw new DomainException("NOT_FOUND", "This record is unavailable.");
    return rows.getFirst();
  }

  private Map<String, Object> eventRow(UUID id) {
    return one("SELECT * FROM event WHERE id=?", id);
  }

  private boolean registered(Actor actor, UUID event) {
    return db.queryForObject(
            "SELECT count(*) FROM registration r JOIN participant p ON p.id=r.participant_id JOIN"
                + " shift s ON s.id=r.shift_id JOIN job j ON j.id=s.job_id WHERE"
                + " p.managing_user_id=? AND j.event_id=?",
            Integer.class,
            actor.id(),
            event)
        > 0;
  }

  private void visible(Actor actor, Map<String, Object> row) {
    UUID org = uuid(row, "organization_id");
    if (actor.organizationId() != null && !actor.organizationId().equals(org)) Access.denied();
    boolean manager = access.organizer(actor, org);
    if (!manager && !registered(actor, uuid(row, "id"))) {
      String visibility = str(row, "visibility");
      if (visibility.equals("HIDDEN")
          || (visibility.equals("PRIVATE") && !access.member(actor, org))) Access.denied();
    }
  }

  public Viewer viewer(Actor actor) {
    String name =
        db.queryForObject("SELECT display_name FROM app_user WHERE id=?", String.class, actor.id());
    var memberships =
        db
            .query(
                "SELECT m.*,o.name FROM membership m JOIN organization o ON o.id=m.organization_id"
                    + " WHERE user_id=?",
                (r, i) ->
                    new Membership(
                        r.getObject("organization_id", UUID.class),
                        r.getString("name"),
                        r.getString("role")),
                actor.id())
            .stream()
            .filter(m ->
                actor.organizationId() == null || m.organizationId().equals(actor.organizationId()))
            .toList();
    return new Viewer(actor.id(), name, memberships);
  }

  public EventPage events(Actor actor, UUID org, int first, int offset) {
    page(first, offset);
    if (!access.member(actor, org)) Access.denied();
    boolean manager = access.organizer(actor, org);
    String filter = manager ? "" : " AND visibility<>'HIDDEN'";
    var items =
        db
            .queryForList(
                "SELECT * FROM event WHERE organization_id=?"
                    + filter
                    + " ORDER BY event_date,id LIMIT ? OFFSET ?",
                org,
                first,
                offset)
            .stream()
            .map(row -> eventData(actor, row))
            .toList();
    int total =
        db.queryForObject(
            "SELECT count(*) FROM event WHERE organization_id=?" + filter, Integer.class, org);
    return new EventPage(items, total);
  }

  public EventData event(Actor actor, UUID id) {
    var row = eventRow(id);
    visible(actor, row);
    return eventData(actor, row);
  }

  public List<SignupGuidance> signupGuidance(Actor actor, UUID eventId) {
    visible(actor, eventRow(eventId));
    // Only the caller's confirmed commitments affect guidance. Do not expose other participants.
    return db.query(
        """
        SELECT s.id,
          (SELECT r.id FROM registration r JOIN participant p ON p.id=r.participant_id
           WHERE p.managing_user_id=? AND r.shift_id=s.id AND r.status='CONFIRMED') AS registration_id,
          EXISTS (SELECT 1 FROM registration r JOIN participant p ON p.id=r.participant_id
                  JOIN shift booked ON booked.id=r.shift_id
                  WHERE p.managing_user_id=? AND r.status='CONFIRMED' AND booked.id<>s.id
                  AND booked.starts_at<s.ends_at AND booked.ends_at>s.starts_at) AS conflict
        FROM shift s JOIN job j ON j.id=s.job_id WHERE j.event_id=?
        ORDER BY s.starts_at,s.id
        """,
        (r, i) ->
            new SignupGuidance(
                r.getObject("id", UUID.class),
                r.getObject("registration_id", UUID.class),
                r.getBoolean("conflict")),
        actor.id(),
        actor.id(),
        eventId);
  }

  private EventData eventData(Actor actor, Map<String, Object> row) {
    return new EventData(
        uuid(row, "id"),
        uuid(row, "organization_id"),
        str(row, "title"),
        str(row, "description"),
        str(row, "location"),
        str(row, "event_date"),
        str(row, "timezone"),
        str(row, "visibility"),
        (Boolean) row.get("signup_open"),
        (Integer) row.get("revision"),
        access.organizer(actor, uuid(row, "organization_id")),
        jobs(uuid(row, "id")));
  }

  private List<JobData> jobs(UUID event) {
    return db.queryForList("SELECT * FROM job WHERE event_id=? ORDER BY id", event).stream()
        .map(this::jobData)
        .toList();
  }

  private JobData jobData(Map<String, Object> row) {
    return new JobData(
        uuid(row, "id"),
        uuid(row, "event_id"),
        str(row, "name"),
        str(row, "description"),
        str(row, "eligibility"),
        db
            .queryForList(
                "SELECT * FROM shift WHERE job_id=? ORDER BY starts_at,id", uuid(row, "id"))
            .stream()
            .map(this::shiftData)
            .toList());
  }

  private ShiftData shiftData(Map<String, Object> row) {
    int filled =
        db.queryForObject(
            "SELECT count(*) FROM registration WHERE shift_id=? AND status='CONFIRMED'",
            Integer.class,
            uuid(row, "id"));
    int cap = (Integer) row.get("capacity");
    return new ShiftData(
        uuid(row, "id"),
        uuid(row, "job_id"),
        instant(row.get("starts_at")).toString(),
        instant(row.get("ends_at")).toString(),
        cap,
        filled,
        cap - filled);
  }

  private UUID eventForShift(UUID shift) {
    return db.queryForObject(
        "SELECT event_id FROM job j JOIN shift s ON s.job_id=j.id WHERE s.id=?", UUID.class, shift);
  }

  private void lockEvent(UUID id) {
    one("SELECT id FROM event WHERE id=? FOR UPDATE", id);
  }

  private void editable(Actor actor, UUID id) {
    lockEvent(id);
    access.requireOrganizer(actor, uuid(eventRow(id), "organization_id"));
    access.write(actor);
  }

  private void noAssignments(UUID event) {
    require(
        db.queryForObject(
                "SELECT count(*) FROM registration r JOIN shift s ON s.id=r.shift_id JOIN job j ON"
                    + " j.id=s.job_id WHERE j.event_id=? AND r.status='CONFIRMED'",
                Integer.class,
                event)
            == 0,
        "CONFLICT",
        "Remove confirmed assignments before changing event timing or staffing structure.");
  }

  @Transactional
  public EventData saveEvent(Actor actor, EventInput input) {
    access.write(actor);
    access.requireOrganizer(actor, input.organizationId());
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
    UUID id = input.id();
    if (id == null) {
      require(!input.signupOpen(), "INVALID_INPUT", "New events start with signup closed.");
      id = UUID.randomUUID();
      db.update(
          "INSERT INTO"
              + " event(id,organization_id,title,description,location,event_date,timezone,visibility)"
              + " VALUES (?,?,?,?,?,?,?,?)",
          id,
          input.organizationId(),
          title,
          description,
          location,
          date,
          input.timezone(),
          input.visibility());
    } else {
      editable(actor, id);
      var row = eventRow(id);
      require(
          uuid(row, "organization_id").equals(input.organizationId()),
          "INVALID_INPUT",
          "An event cannot change organizations.");
      require(
          input.revision() != null && input.revision().equals(row.get("revision")),
          "CONFLICT",
          "This event changed. Reload before saving.");
      if (!str(row, "event_date").equals(input.eventDate())
          || !str(row, "timezone").equals(input.timezone())) {
        noAssignments(id);
        require(
            db.queryForObject(
                    "SELECT count(*) FROM shift s JOIN job j ON j.id=s.job_id WHERE j.event_id=?",
                    Integer.class,
                    id)
                == 0,
            "CONFLICT",
            "Event date and timezone are fixed after shifts are added. Create a new event to"
                + " reschedule.");
      }
      db.update(
          "UPDATE event SET"
              + " title=?,description=?,location=?,event_date=?,timezone=?,visibility=?,signup_open=?,revision=revision+1"
              + " WHERE id=?",
          title,
          description,
          location,
          date,
          input.timezone(),
          input.visibility(),
          input.signupOpen(),
          id);
    }
    return event(actor, id);
  }

  @Transactional
  public JobData saveJob(Actor actor, JobInput input) {
    editable(actor, input.eventId());
    noAssignments(input.eventId());
    String name = text(input.name(), 120, true),
        description = text(input.description(), 1000, false),
        eligibility = text(input.eligibility(), 1000, false);
    UUID id = input.id();
    if (id == null) {
      require(
          db.queryForObject(
                  "SELECT count(*) FROM job WHERE event_id=?", Integer.class, input.eventId())
              < 20,
          "LIMIT_EXCEEDED",
          "Pilot events support at most 20 jobs.");
      id = UUID.randomUUID();
      db.update(
          "INSERT INTO job(id,event_id,name,description,eligibility) VALUES (?,?,?,?,?)",
          id,
          input.eventId(),
          name,
          description,
          eligibility);
    } else {
      require(
          uuid(one("SELECT * FROM job WHERE id=?", id), "event_id").equals(input.eventId()),
          "NOT_FOUND",
          "Job unavailable.");
      db.update(
          "UPDATE job SET name=?,description=?,eligibility=? WHERE id=?",
          name,
          description,
          eligibility,
          id);
    }
    return jobData(one("SELECT * FROM job WHERE id=?", id));
  }

  @Transactional
  public ShiftData saveShift(Actor actor, ShiftInput input) {
    UUID event = uuid(one("SELECT * FROM job WHERE id=?", input.jobId()), "event_id");
    editable(actor, event);
    noAssignments(event);
    Instant start = Instant.parse(input.startsAt()), end = Instant.parse(input.endsAt());
    require(
        end.isAfter(start) && !end.isAfter(start.plusSeconds(86400)),
        "INVALID_INPUT",
        "A shift must last more than zero and at most 24 hours.");
    require(
        start
            .atZone(ZoneId.of(str(eventRow(event), "timezone")))
            .toLocalDate()
            .equals(LocalDate.parse(str(eventRow(event), "event_date"))),
        "INVALID_INPUT",
        "Shift start must be on the event date in its timezone.");
    require(
        input.capacity() >= 1 && input.capacity() <= 1000,
        "INVALID_INPUT",
        "Capacity must be 1–1000.");
    UUID id = input.id();
    if (id == null) {
      require(
          db.queryForObject(
                  "SELECT count(*) FROM shift WHERE job_id=?", Integer.class, input.jobId())
              < 100,
          "LIMIT_EXCEEDED",
          "Pilot jobs support at most 100 shifts.");
      id = UUID.randomUUID();
      db.update(
          "INSERT INTO shift(id,job_id,starts_at,ends_at,capacity) VALUES (?,?,?,?,?)",
          id,
          input.jobId(),
          OffsetDateTime.parse(start.toString()),
          OffsetDateTime.parse(end.toString()),
          input.capacity());
    } else {
      require(
          uuid(one("SELECT * FROM shift WHERE id=?", id), "job_id").equals(input.jobId()),
          "NOT_FOUND",
          "Shift unavailable.");
      db.update(
          "UPDATE shift SET starts_at=?,ends_at=?,capacity=? WHERE id=?",
          OffsetDateTime.parse(start.toString()),
          OffsetDateTime.parse(end.toString()),
          input.capacity(),
          id);
    }
    return shiftData(one("SELECT * FROM shift WHERE id=?", id));
  }

  private RegistrationData registration(Actor actor, UUID id) {
    var row =
        one(
            "SELECT r.*,u.display_name,p.managing_user_id FROM registration r JOIN participant p ON"
                + " p.id=r.participant_id JOIN app_user u ON u.id=p.managing_user_id WHERE r.id=?",
            id);
    UUID shiftId = uuid(row, "shift_id"), eventId = eventForShift(shiftId);
    var event = eventRow(eventId);
    if (!uuid(row, "managing_user_id").equals(actor.id()))
      access.requireOrganizer(actor, uuid(event, "organization_id"));
    if (actor.organizationId() != null
        && !actor.organizationId().equals(uuid(event, "organization_id"))) Access.denied();
    return new RegistrationData(
        id,
        str(row, "status"),
        str(row, "display_name"),
        eventData(actor, event),
        shiftData(one("SELECT * FROM shift WHERE id=?", shiftId)));
  }

  public List<RegistrationData> myRegistrations(Actor actor, int first, int offset) {
    page(first, offset);
    return db
        .queryForList(
            "SELECT r.id,e.organization_id FROM registration r JOIN participant p ON"
                + " p.id=r.participant_id JOIN shift s ON s.id=r.shift_id JOIN job j ON"
                + " j.id=s.job_id JOIN event e ON e.id=j.event_id WHERE p.managing_user_id=? ORDER"
                + " BY r.created_at DESC,r.id LIMIT ? OFFSET ?",
            actor.id(),
            first,
            offset)
        .stream()
        .filter(r ->
            actor.organizationId() == null
                || actor.organizationId().equals(uuid(r, "organization_id")))
        .map(r -> registration(actor, uuid(r, "id")))
        .toList();
  }

  public List<RegistrationData> coverage(Actor actor, UUID event, int first, int offset) {
    page(first, offset);
    access.requireOrganizer(actor, uuid(eventRow(event), "organization_id"));
    return db
        .queryForList(
            "SELECT r.id FROM registration r JOIN shift s ON s.id=r.shift_id JOIN job j ON"
                + " j.id=s.job_id WHERE j.event_id=? AND r.status='CONFIRMED' ORDER BY"
                + " r.created_at,r.id LIMIT ? OFFSET ?",
            event,
            first,
            offset)
        .stream()
        .map(r -> registration(actor, uuid(r, "id")))
        .toList();
  }

  public List<Member> members(Actor actor, UUID org, int first, int offset) {
    page(first, offset);
    access.requireOrganizer(actor, org);
    return db.query(
        "SELECT u.*,m.role FROM membership m JOIN app_user u ON u.id=m.user_id WHERE"
            + " m.organization_id=? ORDER BY u.display_name,u.id LIMIT ? OFFSET ?",
        (r, i) ->
            new Member(
                r.getObject("id", UUID.class), r.getString("display_name"), r.getString("role")),
        org,
        first,
        offset);
  }

  @Transactional
  public RegistrationData signup(
      Actor actor, UUID shift, UUID targetUser, UUID request, boolean assignment) {
    access.write(actor);
    UUID event = eventForShift(shift);
    require(event != null, "NOT_FOUND", "Shift unavailable.");
    lockEvent(event);
    var eventRow = eventRow(event);
    UUID org = uuid(eventRow, "organization_id");
    if (assignment) access.requireOrganizer(actor, org);
    else if (!access.member(actor, org)) Access.denied();
    require(
        db.queryForObject(
                "SELECT count(*) FROM membership WHERE user_id=? AND organization_id=?",
                Integer.class,
                targetUser,
                org)
            == 1,
        "FORBIDDEN",
        "Participant is not a member of this organization.");
    var participant =
        one("SELECT id FROM participant WHERE managing_user_id=? FOR UPDATE", targetUser);
    UUID participantId = uuid(participant, "id");
    String fingerprint =
        SecurityConfig.digest((assignment ? "assign" : "signup") + shift + targetUser);
    var receipts =
        db.queryForList(
            "SELECT * FROM mutation_receipt WHERE user_id=? AND request_id=?", actor.id(), request);
    if (!receipts.isEmpty()) {
      var receipt = receipts.getFirst();
      require(
          str(receipt, "fingerprint").equals(fingerprint),
          "CONFLICT",
          "This request ID was already used for another action.");
      return registration(actor, uuid(receipt, "result_id"));
    }
    require(
        (Boolean) eventRow.get("signup_open") && !str(eventRow, "visibility").equals("HIDDEN"),
        "CLOSED",
        "Signup is closed for this event.");
    var slot = one("SELECT * FROM shift WHERE id=? FOR UPDATE", shift);
    require(instant(slot.get("ends_at")).isAfter(Instant.now()), "CLOSED", "This shift has ended.");
    var previous =
        db.queryForList(
            "SELECT * FROM registration WHERE participant_id=? AND shift_id=? AND"
                + " status='CONFIRMED'",
            participantId,
            shift);
    UUID id = previous.isEmpty() ? UUID.randomUUID() : uuid(previous.getFirst(), "id");
    if (previous.isEmpty() || !str(previous.getFirst(), "status").equals("CONFIRMED")) {
      int filled =
          db.queryForObject(
              "SELECT count(*) FROM registration WHERE shift_id=? AND status='CONFIRMED'",
              Integer.class,
              shift);
      require(
          filled < (Integer) slot.get("capacity"),
          "FULL",
          "This shift is full. Please choose another shift.");
      int overlap =
          db.queryForObject(
              "SELECT count(*) FROM registration r JOIN shift s ON s.id=r.shift_id WHERE"
                  + " r.participant_id=? AND r.status='CONFIRMED' AND s.starts_at<? AND"
                  + " s.ends_at>?",
              Integer.class,
              participantId,
              slot.get("ends_at"),
              slot.get("starts_at"));
      require(
          overlap == 0,
          "OVERLAP",
          assignment
              ? "This participant already has a shift at that time."
              : "You already have a shift at this time. View My registrations to review your"
                  + " bookings.");
      db.update(
          "INSERT INTO registration(id,participant_id,shift_id,status) VALUES (?,?,?,'CONFIRMED')",
          id,
          participantId,
          shift);
    }
    db.update(
        "INSERT INTO mutation_receipt(user_id,request_id,fingerprint,result_id) VALUES (?,?,?,?)",
        actor.id(),
        request,
        fingerprint,
        id);
    return registration(actor, id);
  }

  @Transactional
  public RegistrationData cancel(Actor actor, UUID id, boolean manager) {
    access.write(actor);
    var row =
        one(
            "SELECT r.*,p.managing_user_id FROM registration r JOIN participant p ON"
                + " p.id=r.participant_id WHERE r.id=?",
            id);
    UUID event = eventForShift(uuid(row, "shift_id"));
    lockEvent(event);
    if (manager) access.requireOrganizer(actor, uuid(eventRow(event), "organization_id"));
    else if (!uuid(row, "managing_user_id").equals(actor.id())) Access.denied();
    if (actor.organizationId() != null
        && !actor.organizationId().equals(uuid(eventRow(event), "organization_id")))
      Access.denied();
    one("SELECT id FROM participant WHERE id=? FOR UPDATE", uuid(row, "participant_id"));
    db.update("UPDATE registration SET status='CANCELLED',updated_at=now() WHERE id=?", id);
    return registration(actor, id);
  }
}
