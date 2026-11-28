package org.whitesborobandboosters.rallyboro.organization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.whitesborobandboosters.rallyboro.security.Access;
import org.whitesborobandboosters.rallyboro.security.Access.Actor;
import org.whitesborobandboosters.rallyboro.security.Accounts;
import org.whitesborobandboosters.rallyboro.security.SecurityConfig;
import org.whitesborobandboosters.rallyboro.staffing.DomainException;
import org.whitesborobandboosters.rallyboro.staffing.Models.Member;

@Service
@Transactional(readOnly = true)
public class Organizations {
  private final JdbcTemplate db;
  private final Access access;

  public Organizations(JdbcTemplate db, Access access) {
    this.db = db;
    this.access = access;
  }

  public record OrganizationData(UUID id, String name) {}

  public record Invitation(
      UUID id,
      UUID organizationId,
      String organizationName,
      boolean canManage,
      boolean canOrganize,
      UUID eventId,
      String expiresAt,
      String status) {}

  public record InvitationGrant(String token, Invitation invitation) {}

  public record AuditEntry(
      UUID id, String actorName, String action, UUID targetId, String createdAt) {}

  private static void require(boolean valid, String code, String message) {
    if (!valid) throw new DomainException(code, message);
  }

  private static void page(int first, int offset) {
    require(
        first >= 1 && first <= 100 && offset >= 0 && offset <= 10000,
        "INVALID_INPUT",
        "Use a page size of 1–100 and offset of 0–10000.");
  }

  private Map<String, Object> organization(UUID id, boolean lock) {
    var rows =
        db.queryForList("SELECT * FROM organization WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
    require(!rows.isEmpty(), "NOT_FOUND", "This organization is unavailable.");
    return rows.getFirst();
  }

  private void manager(Actor actor, UUID org) {
    Access.requireSession(actor);
    if (!access.manageOrganization(actor, org)) Access.denied();
  }

  private void audit(Actor actor, UUID org, String action, UUID target) {
    db.update(
        "INSERT INTO access_audit(id,organization_id,actor_id,action,target_id) VALUES (?,?,?,?,?)",
        UUID.randomUUID(),
        org,
        actor.id(),
        action,
        target);
  }

  @Transactional
  public OrganizationData create(Actor actor, String name) {
    Access.requireSession(actor);
    require(
        name != null && !name.isBlank() && name.length() <= 120,
        "INVALID_INPUT",
        "Organization name must be 1–120 characters.");
    db.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE", actor.id());
    require(
        db.queryForObject(
                "SELECT count(*) FROM membership WHERE user_id=? AND can_manage",
                Integer.class,
                actor.id())
            < 20,
        "LIMIT_EXCEEDED",
        "You can manage up to 20 organizations in this release.");
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO organization(id,name) VALUES (?,?)", id, name.strip());
    db.update(
        "INSERT INTO membership(user_id,organization_id,role,can_manage,can_organize) VALUES"
            + " (?,?,'ORGANIZER',true,true)",
        actor.id(),
        id);
    audit(actor, id, "ORGANIZATION_CREATED", id);
    return new OrganizationData(id, name.strip());
  }

  @Transactional
  public InvitationGrant invite(
      Actor actor, UUID org, boolean manage, boolean organize, UUID event) {
    organization(org, true);
    manager(actor, org);
    if (event != null) {
      require(
          !manage && !organize,
          "INVALID_INPUT",
          "Choose an organization permission or one event, not both.");
      require(
          db.queryForObject(
                  "SELECT count(*) FROM event WHERE id=? AND organization_id=?",
                  Integer.class,
                  event,
                  org)
              == 1,
          "NOT_FOUND",
          "This event is unavailable.");
    }
    require(
        db.queryForObject(
                "SELECT count(*) FROM organization_invitation WHERE organization_id=? AND NOT"
                    + " revoked AND accepted_by IS NULL AND expires_at>now()",
                Integer.class,
                org)
            < 100,
        "LIMIT_EXCEEDED",
        "Revoke an invitation before creating another.");
    UUID id = UUID.randomUUID();
    String token = Accounts.secret();
    db.update(
        "INSERT INTO"
            + " organization_invitation(id,organization_id,created_by,token_hash,can_manage,can_organize,event_id,expires_at)"
            + " VALUES (?,?,?,?,?,?,?,now()+interval '7 days')",
        id,
        org,
        actor.id(),
        SecurityConfig.digest(token),
        manage,
        organize,
        event);
    audit(actor, org, "INVITATION_CREATED", id);
    return new InvitationGrant(token, invitationById(id));
  }

  private Invitation invitationById(UUID id) {
    return db.query(
            "SELECT i.*,o.name,EXISTS(SELECT 1 FROM membership m WHERE"
                + " m.organization_id=i.organization_id AND m.user_id=i.created_by AND"
                + " m.can_manage) AS issuer_active FROM organization_invitation i JOIN organization"
                + " o ON o.id=i.organization_id WHERE i.id=?",
            (r, i) -> {
              var expiry = r.getObject("expires_at", OffsetDateTime.class);
              String status =
                  r.getObject("accepted_by") != null
                      ? "ACCEPTED"
                      : r.getBoolean("revoked")
                          ? "REVOKED"
                          : !expiry.isAfter(OffsetDateTime.now())
                              ? "EXPIRED"
                              : !r.getBoolean("issuer_active") ? "ISSUER_REMOVED" : "AVAILABLE";
              return new Invitation(
                  r.getObject("id", UUID.class),
                  r.getObject("organization_id", UUID.class),
                  r.getString("name"),
                  r.getBoolean("can_manage"),
                  r.getBoolean("can_organize"),
                  r.getObject("event_id", UUID.class),
                  expiry.toInstant().toString(),
                  status);
            },
            id)
        .getFirst();
  }

  private Map<String, Object> tokenRow(String token) {
    require(
        token != null && token.matches("[A-Za-z0-9_-]{43}"),
        "NOT_FOUND",
        "This invitation is unavailable.");
    var rows =
        db.queryForList(
            "SELECT * FROM organization_invitation WHERE token_hash=?",
            SecurityConfig.digest(token));
    require(!rows.isEmpty(), "NOT_FOUND", "This invitation is unavailable.");
    return rows.getFirst();
  }

  private Invitation usable(Actor actor, Map<String, Object> row) {
    var invitation = invitationById((UUID) row.get("id"));
    boolean own =
        actor.id().equals(row.get("accepted_by"))
            && access.member(actor, invitation.organizationId());
    require(
        invitation.status().equals("AVAILABLE") || (invitation.status().equals("ACCEPTED") && own),
        "NOT_FOUND",
        "This invitation is unavailable.");
    return invitation;
  }

  public Invitation preview(Actor actor, String token) {
    Access.requireSession(actor);
    return usable(actor, tokenRow(token));
  }

  @Transactional
  public OrganizationData accept(Actor actor, String token) {
    Access.requireSession(actor);
    var initial = tokenRow(token);
    UUID org = (UUID) initial.get("organization_id"), id = (UUID) initial.get("id");
    organization(org, true);
    var row =
        db.queryForList("SELECT * FROM organization_invitation WHERE id=? FOR UPDATE", id)
            .getFirst();
    var invitation = usable(actor, row);
    if (row.get("accepted_by") == null) {
      db.update(
          "INSERT INTO membership(user_id,organization_id,role,can_manage,can_organize) VALUES"
              + " (?,?,?,?,?) ON CONFLICT(user_id,organization_id) DO UPDATE SET"
              + " can_manage=membership.can_manage OR"
              + " excluded.can_manage,can_organize=membership.can_organize OR"
              + " excluded.can_organize,role=CASE WHEN membership.can_manage OR"
              + " membership.can_organize OR excluded.can_manage OR excluded.can_organize THEN"
              + " 'ORGANIZER' ELSE 'VOLUNTEER' END",
          actor.id(),
          org,
          invitation.canManage() || invitation.canOrganize() ? "ORGANIZER" : "VOLUNTEER",
          invitation.canManage(),
          invitation.canOrganize());
      if (invitation.eventId() != null)
        db.update(
            "INSERT INTO event_manager(event_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",
            invitation.eventId(),
            actor.id());
      db.update("UPDATE organization_invitation SET accepted_by=? WHERE id=?", actor.id(), id);
      audit(actor, org, "INVITATION_ACCEPTED", id);
    }
    return new OrganizationData(org, invitation.organizationName());
  }

  public List<Invitation> invitations(Actor actor, UUID org, int first, int offset) {
    page(first, offset);
    manager(actor, org);
    return db
        .queryForList(
            "SELECT id FROM organization_invitation WHERE organization_id=? ORDER BY created_at"
                + " DESC,id LIMIT ? OFFSET ?",
            org,
            first,
            offset)
        .stream()
        .map(r -> invitationById((UUID) r.get("id")))
        .toList();
  }

  @Transactional
  public boolean revoke(Actor actor, UUID org, UUID id) {
    organization(org, true);
    manager(actor, org);
    int changed =
        db.update(
            "UPDATE organization_invitation SET revoked=true WHERE id=? AND organization_id=? AND"
                + " NOT revoked AND accepted_by IS NULL",
            id,
            org);
    if (changed > 0) audit(actor, org, "INVITATION_REVOKED", id);
    return true;
  }

  @Transactional
  public boolean membership(
      Actor actor, UUID org, UUID user, boolean manage, boolean organize, boolean remove) {
    organization(org, true);
    manager(actor, org);
    var rows =
        db.queryForList(
            "SELECT * FROM membership WHERE user_id=? AND organization_id=?", user, org);
    require(!rows.isEmpty(), "NOT_FOUND", "This membership is unavailable.");
    if ((Boolean) rows.getFirst().get("can_manage") && (remove || !manage))
      require(
          db.queryForObject(
                  "SELECT count(*) FROM membership WHERE organization_id=? AND can_manage",
                  Integer.class,
                  org)
              > 1,
          "CONFLICT",
          "Keep at least one organization manager.");
    if ((Boolean) rows.getFirst().get("can_manage") && (remove || !manage)) {
      db.update(
          "UPDATE organization_invitation SET revoked=true WHERE organization_id=? AND created_by=?"
              + " AND accepted_by IS NULL",
          org,
          user);
    }
    if (remove) {
      db.update(
          "DELETE FROM event_manager WHERE user_id=? AND event_id IN (SELECT id FROM event WHERE"
              + " organization_id=?)",
          user,
          org);
      db.update("DELETE FROM membership WHERE user_id=? AND organization_id=?", user, org);
      db.update(
          "UPDATE agent_token SET revoked=true WHERE user_id=? AND organization_id=?", user, org);
    } else
      db.update(
          "UPDATE membership SET can_manage=?,can_organize=?,role=? WHERE user_id=? AND"
              + " organization_id=?",
          manage,
          organize,
          manage || organize ? "ORGANIZER" : "VOLUNTEER",
          user,
          org);
    audit(actor, org, remove ? "MEMBER_REMOVED" : "PERMISSIONS_CHANGED", user);
    return true;
  }

  private UUID eventOrganization(UUID event) {
    var rows = db.queryForList("SELECT organization_id FROM event WHERE id=?", event);
    require(!rows.isEmpty(), "NOT_FOUND", "This event is unavailable.");
    return (UUID) rows.getFirst().get("organization_id");
  }

  @Transactional
  public boolean eventManager(Actor actor, UUID event, UUID user, boolean enabled) {
    UUID org = eventOrganization(event);
    organization(org, true);
    manager(actor, org);
    require(
        access.member(new Actor(user, null, false, true), org),
        "NOT_FOUND",
        "Select an organization member.");
    if (enabled)
      db.update(
          "INSERT INTO event_manager(event_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",
          event,
          user);
    else db.update("DELETE FROM event_manager WHERE event_id=? AND user_id=?", event, user);
    audit(actor, org, enabled ? "EVENT_MANAGER_GRANTED" : "EVENT_MANAGER_REMOVED", user);
    return true;
  }

  public List<Member> eventManagers(Actor actor, UUID event, int first, int offset) {
    page(first, offset);
    manager(actor, eventOrganization(event));
    return db.query(
        "SELECT u.id,u.display_name,m.role,m.can_manage,m.can_organize FROM event_manager g JOIN"
            + " app_user u ON u.id=g.user_id JOIN event e ON e.id=g.event_id JOIN membership m ON"
            + " m.user_id=u.id AND m.organization_id=e.organization_id WHERE g.event_id=? ORDER BY"
            + " u.display_name,u.id LIMIT ? OFFSET ?",
        (r, i) ->
            new Member(
                r.getObject("id", UUID.class),
                r.getString("display_name"),
                r.getString("role"),
                r.getBoolean("can_manage"),
                r.getBoolean("can_manage") || r.getBoolean("can_organize")),
        event,
        first,
        offset);
  }

  public List<AuditEntry> audit(Actor actor, UUID org, int first, int offset) {
    page(first, offset);
    manager(actor, org);
    return db.query(
        "SELECT a.*,u.display_name FROM access_audit a JOIN app_user u ON u.id=a.actor_id WHERE"
            + " a.organization_id=? ORDER BY a.created_at DESC,a.id LIMIT ? OFFSET ?",
        (r, i) ->
            new AuditEntry(
                r.getObject("id", UUID.class),
                r.getString("display_name"),
                r.getString("action"),
                r.getObject("target_id", UUID.class),
                r.getObject("created_at", OffsetDateTime.class).toInstant().toString()),
        org,
        first,
        offset);
  }
}
