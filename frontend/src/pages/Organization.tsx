import { useState, type FormEvent } from "react";
import { client, message } from "../api";
import { Alert, Loader, LoadError, useLoad, useSession } from "../ui";
import * as G from "../generated";
function Pager({
  offset,
  count,
  onChange,
  label,
}: {
  offset: number;
  count: number;
  onChange: (value: number) => void;
  label: string;
}) {
  return (
    <div className="pagination">
      <button
        disabled={!offset}
        onClick={() => onChange(Math.max(0, offset - 25))}
      >
        Previous {label}
      </button>
      <button disabled={count < 25} onClick={() => onChange(offset + 25)}>
        Next {label}
      </button>
    </div>
  );
}
export function Organization() {
  const { organization, viewer, reload } = useSession();
  const org = organization.organizationId;
  const [memberOffset, setMemberOffset] = useState(0),
    [inviteOffset, setInviteOffset] = useState(0),
    [auditOffset, setAuditOffset] = useState(0),
    [eventOffset, setEventOffset] = useState(0),
    [managerOffset, setManagerOffset] = useState(0);
  const [eventId, setEventId] = useState("");
  const members = useLoad(
    () =>
      client.request(G.OrganizationMembersDocument, {
        organizationId: org,
        offset: memberOffset,
      }),
    [org, memberOffset],
  );
  const invitations = useLoad(
    () =>
      client.request(G.OrganizationInvitationsDocument, {
        organizationId: org,
        offset: inviteOffset,
      }),
    [org, inviteOffset],
  );
  const events = useLoad(
    () =>
      client.request(G.OrganizationEventsDocument, {
        organizationId: org,
        offset: eventOffset,
      }),
    [org, eventOffset],
  );
  const audit = useLoad(
    () =>
      client.request(G.OrganizationAuditDocument, {
        organizationId: org,
        offset: auditOffset,
      }),
    [org, auditOffset],
  );
  const managers = useLoad(
    () =>
      eventId
        ? client.request(G.EventManagersDocument, {
            eventId,
            offset: managerOffset,
          })
        : Promise.resolve(null),
    [org, eventId, managerOffset],
  );
  const [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [link, setLink] = useState("");
  const [busy, setBusy] = useState(false),
    [scope, setScope] = useState("volunteer");
  function refresh() {
    members.refresh();
    invitations.refresh();
    audit.refresh();
    managers.refresh();
  }
  async function run(action: () => Promise<unknown>, success: string) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      await action();
      setNotice(success);
      refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function invite(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setLink("");
    await run(async () => {
      const result = await client.request(G.CreateInvitationDocument, {
        organizationId: org,
        canManage: scope === "manage",
        canOrganize: scope === "organize",
        eventId: scope === "event" ? eventId : null,
      });
      setLink(
        new URL(
          "/invite/" + result.createInvitation.token,
          window.location.origin,
        ).href,
      );
    }, "Invitation created.");
  }
  async function permissions(e: FormEvent<HTMLFormElement>, userId: string) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    await run(async () => {
      await client.request(G.SetPermissionsDocument, {
        organizationId: org,
        userId,
        canManage: data.get("manage") === "on",
        canOrganize: data.get("organize") === "on",
      });
      if (userId === viewer.id) reload();
    }, "Permissions saved.");
  }
  if (!organization.canManage)
    return <Alert>Organization management permission is required.</Alert>;
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Organization</h1>
          <p className="muted">{organization.organizationName}</p>
        </div>
      </div>
      {error && <Alert>{error}</Alert>}
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      {link && (
        <section className="panel">
          <h2>Share invitation</h2>
          <p>
            This single-use link expires in seven days. Share it only with the
            intended recipient. It is shown once.
          </p>
          <textarea aria-label="Invitation link" value={link} readOnly />
          <button onClick={() => setLink("")}>Dismiss invitation link</button>
        </section>
      )}
      <form className="panel" onSubmit={invite}>
        <h2>Invite someone</h2>
        <label>
          Invitation permissions
          <select value={scope} onChange={(e) => setScope(e.target.value)}>
            <option value="volunteer">Participate as a volunteer</option>
            <option value="organize">Create and manage all events</option>
            <option value="event">Manage one event</option>
            <option value="manage">
              Manage organization, members, and events
            </option>
          </select>
        </label>
        {scope === "event" && (
          <p className="small muted">
            Choose the event in Event access below before creating this
            invitation.
          </p>
        )}
        <button
          className="primary"
          disabled={busy || (scope === "event" && !eventId)}
        >
          Create invitation
        </button>
      </form>
      <section className="panel">
        <h2>Members</h2>
        {members.loading ? (
          <Loader />
        ) : members.error ? (
          <LoadError error={members.error} retry={members.refresh} />
        ) : (
          members.data?.members.map((person) => (
            <form
              className="member-row"
              key={person.id + person.canManage + person.canOrganize}
              onSubmit={(e) => permissions(e, person.id)}
            >
              <h3>
                {person.displayName}
                {person.id === viewer.id ? " (you)" : ""}
              </h3>
              <label className="check">
                <input
                  type="checkbox"
                  name="manage"
                  defaultChecked={person.canManage}
                />
                Manage organization and permissions
              </label>
              <label className="check">
                <input
                  type="checkbox"
                  name="organize"
                  defaultChecked={person.canOrganize}
                />
                Create and manage all events
              </label>
              <div className="action-row">
                <button disabled={busy}>Save permissions</button>
                <button
                  type="button"
                  disabled={busy}
                  onClick={() => {
                    if (
                      confirm(
                        "Remove " +
                          person.displayName +
                          " from this organization?",
                      )
                    )
                      void run(async () => {
                        await client.request(G.RemoveMemberDocument, {
                          organizationId: org,
                          userId: person.id,
                        });
                        if (person.id === viewer.id) reload();
                      }, "Member removed.");
                  }}
                >
                  Remove member
                </button>
              </div>
            </form>
          ))
        )}
        <Pager
          offset={memberOffset}
          count={members.data?.members.length ?? 0}
          onChange={setMemberOffset}
          label="members"
        />
      </section>
      <section className="panel">
        <h2>Event access</h2>
        {events.error && <Alert>{events.error}</Alert>}
        <label>
          Event
          <select
            value={eventId}
            onChange={(e) => {
              setEventId(e.target.value);
              setManagerOffset(0);
            }}
          >
            <option value="">Choose an event</option>
            {events.data?.events.items.map((event) => (
              <option key={event.id} value={event.id}>
                {event.title}
              </option>
            ))}
          </select>
        </label>
        <Pager
          offset={eventOffset}
          count={events.data?.events.items.length ?? 0}
          onChange={setEventOffset}
          label="events"
        />
        {eventId && (
          <>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                const data = new FormData(e.currentTarget);
                void run(
                  () =>
                    client.request(G.SetEventManagerDocument, {
                      eventId,
                      userId: String(data.get("user")),
                      enabled: true,
                    }),
                  "Event access granted.",
                );
              }}
            >
              <label>
                Member to organize this event
                <select name="user" required>
                  <option value="">Choose a member</option>
                  {members.data?.members.map((p) => (
                    <option value={p.id} key={p.id}>
                      {p.displayName}
                    </option>
                  ))}
                </select>
              </label>
              <p className="small muted">
                The member picker uses the current Members page. This grant
                applies only to the selected event.
              </p>
              <button disabled={busy}>Grant event access</button>
            </form>
            {managers.loading ? (
              <Loader />
            ) : managers.error ? (
              <LoadError error={managers.error} retry={managers.refresh} />
            ) : (
              managers.data?.eventManagers.map((p) => (
                <div className="shift-row" key={p.id}>
                  <strong>{p.displayName}</strong>
                  <button
                    disabled={busy}
                    onClick={() =>
                      void run(
                        () =>
                          client.request(G.SetEventManagerDocument, {
                            eventId,
                            userId: p.id,
                            enabled: false,
                          }),
                        "Event grant removed.",
                      )
                    }
                  >
                    Remove event grant
                  </button>
                </div>
              ))
            )}
            <p className="small muted">
              Organization managers and all-event organizers retain their
              organization-wide access.
            </p>
            <Pager
              offset={managerOffset}
              count={managers.data?.eventManagers.length ?? 0}
              onChange={setManagerOffset}
              label="event grants"
            />
          </>
        )}
      </section>
      <section className="panel">
        <h2>Invitations</h2>
        {invitations.loading ? (
          <Loader />
        ) : invitations.error ? (
          <LoadError error={invitations.error} retry={invitations.refresh} />
        ) : (
          invitations.data?.organizationInvitations.map((invitation) => (
            <div className="shift-row" key={invitation.id}>
              <div>
                <strong>
                  {invitation.canManage
                    ? "Organization management"
                    : invitation.canOrganize
                      ? "All events"
                      : invitation.eventId
                        ? "One event"
                        : "Volunteer"}
                </strong>
                <span>
                  {invitation.status} · Expires{" "}
                  {new Date(invitation.expiresAt).toLocaleString()}
                </span>
              </div>
              <button
                disabled={busy || invitation.status !== "AVAILABLE"}
                onClick={() =>
                  void run(
                    () =>
                      client.request(G.RevokeInvitationDocument, {
                        organizationId: org,
                        id: invitation.id,
                      }),
                    "Invitation revoked.",
                  )
                }
              >
                Revoke invitation
              </button>
            </div>
          ))
        )}
        <Pager
          offset={inviteOffset}
          count={invitations.data?.organizationInvitations.length ?? 0}
          onChange={setInviteOffset}
          label="invitations"
        />
      </section>
      <section className="panel">
        <h2>Access history</h2>
        {audit.loading ? (
          <Loader />
        ) : audit.error ? (
          <LoadError error={audit.error} retry={audit.refresh} />
        ) : (
          audit.data?.organizationAudit.map((entry) => (
            <div className="member-row" key={entry.id}>
              <strong>{entry.action.toLowerCase().replaceAll("_", " ")}</strong>
              <p className="small muted">
                {entry.actorName} · {new Date(entry.createdAt).toLocaleString()}
              </p>
            </div>
          ))
        )}
        <Pager
          offset={auditOffset}
          count={audit.data?.organizationAudit.length ?? 0}
          onChange={setAuditOffset}
          label="history"
        />
      </section>
    </>
  );
}
