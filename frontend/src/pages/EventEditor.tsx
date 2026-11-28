import { useState, type FormEvent } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { client, message } from "../api";
import { useSession, Alert, useLoad, Loader, LoadError } from "../ui";
import * as G from "../generated";
export function EventEditor() {
  const { id } = useParams();
  const { organization } = useSession();
  const navigate = useNavigate();
  const load = useLoad(
    async () => (id ? client.request(G.EventDocument, { id }) : null),
    [id],
  );
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const event = load.data?.event;
  async function save(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setError("");
    setNotice("");
    setBusy(true);
    try {
      const result = await client.request(G.SaveEventDocument, {
        input: {
          id: event?.id ?? null,
          organizationId: event?.organizationId ?? organization.organizationId,
          title: String(f.get("title")),
          description: String(f.get("description")),
          location: String(f.get("location")),
          eventDate: String(f.get("date")),
          timezone: String(f.get("timezone")),
          visibility: String(f.get("visibility")) as G.Visibility,
          signupOpen: event ? f.get("open") === "on" : false,
          revision: event?.revision ?? null,
        },
      });
      setNotice("Event saved.");
      if (!id) navigate("/events/" + result.saveEvent.id + "/edit");
      else load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function saveJob(e: FormEvent<HTMLFormElement>, jobId: string | null) {
    e.preventDefault();
    if (!event) return;
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      await client.request(G.SaveJobDocument, {
        input: {
          id: jobId,
          eventId: event.id,
          name: String(f.get("name")),
          description: String(f.get("description")),
          eligibility: String(f.get("eligibility")),
        },
      });
      setNotice("Job saved.");
      load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function saveShift(
    e: FormEvent<HTMLFormElement>,
    jobId: string,
    shiftId: string | null,
  ) {
    e.preventDefault();
    if (!event) return;
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      const { Temporal } = await import("@js-temporal/polyfill");
      const toInstant = (field: string) =>
        Temporal.PlainDateTime.from(String(f.get(field)))
          .toZonedDateTime(event.timezone, { disambiguation: "reject" })
          .toInstant()
          .toString();
      await client.request(G.SaveShiftDocument, {
        input: {
          id: shiftId,
          jobId,
          startsAt: toInstant("start"),
          endsAt: toInstant("end"),
          capacity: Number(f.get("capacity")),
        },
      });
      setNotice("Shift saved.");
      load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  if (load.loading) return <Loader />;
  if (load.error) return <LoadError error={load.error} retry={load.refresh} />;
  if (organization.role !== "ORGANIZER" || (event && !event.canManage))
    return <Alert>Organizer access is required.</Alert>;
  const assigned = event?.jobs.some((j) => j.shifts.some((s) => s.filled > 0));
  const scheduled = event?.jobs.some((job) => job.shifts.length > 0);
  function localInput(iso: string) {
    const parts = new Intl.DateTimeFormat("sv-SE", {
      timeZone: event?.timezone,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      hourCycle: "h23",
    }).format(new Date(iso));
    return parts.replace(" ", "T");
  }
  function shiftForm(jobId: string, shift: G.SlotFragment | null) {
    return (
      <form
        className="shift-editor"
        key={shift?.id ?? "new"}
        onSubmit={(e) => saveShift(e, jobId, shift?.id ?? null)}
      >
        <fieldset disabled={busy || assigned}>
          <legend>{shift ? "Edit shift" : "Add a shift"}</legend>
          <div className="form-grid">
            <label>
              Start ({event?.timezone})
              <input
                name="start"
                type="datetime-local"
                required
                defaultValue={
                  shift
                    ? localInput(shift.startsAt)
                    : event?.eventDate + "T09:00"
                }
              />
            </label>
            <label>
              End ({event?.timezone})
              <input
                name="end"
                type="datetime-local"
                required
                defaultValue={
                  shift ? localInput(shift.endsAt) : event?.eventDate + "T11:00"
                }
              />
            </label>
            <label>
              Capacity
              <input
                name="capacity"
                type="number"
                min={1}
                max={1000}
                defaultValue={shift?.capacity ?? 2}
                required
              />
            </label>
          </div>
          <button>{shift ? "Save shift" : "Add shift"}</button>
        </fieldset>
      </form>
    );
  }
  return (
    <>
      <Link className="back" to={event ? "/events/" + event.id : "/"}>
        ← {event ? "View event" : "All events"}
      </Link>
      <div className="page-heading">
        <div>
          <h1>{event ? "Edit event" : "Create an event"}</h1>
        </div>
      </div>
      {error && <Alert>{error}</Alert>}
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <form
        className="panel event-form"
        key={event?.revision ?? "new"}
        onSubmit={save}
      >
        <h2>Event details</h2>
        <fieldset disabled={busy}>
          <label>
            Event title
            <input
              name="title"
              maxLength={120}
              defaultValue={event?.title ?? ""}
              required
              placeholder="e.g. Friday night concessions"
            />
          </label>
          <label>
            Description
            <textarea
              name="description"
              maxLength={2000}
              defaultValue={event?.description ?? ""}
              rows={3}
            />
          </label>
          <div className="form-grid">
            <label>
              Date
              <input
                name="date"
                type="date"
                defaultValue={event?.eventDate ?? ""}
                required
                disabled={scheduled}
              />
              {scheduled && (
                <input type="hidden" name="date" value={event?.eventDate} />
              )}
            </label>
            <label>
              Timezone
              <input
                name="timezone"
                defaultValue={
                  event?.timezone ??
                  Intl.DateTimeFormat().resolvedOptions().timeZone
                }
                required
                disabled={scheduled}
              />
              {scheduled && (
                <input type="hidden" name="timezone" value={event?.timezone} />
              )}
            </label>
          </div>
          {scheduled && (
            <p className="small muted">
              Date and timezone are fixed once shifts are added. Create a new
              event to reschedule.
            </p>
          )}
          <label>
            Location
            <input
              name="location"
              maxLength={200}
              defaultValue={event?.location ?? ""}
            />
          </label>
          <div className="form-grid">
            <label>
              Visibility
              <select
                name="visibility"
                defaultValue={event?.visibility ?? "HIDDEN"}
              >
                <option value="HIDDEN">Hidden — organizers only</option>
                <option value="PRIVATE">Private — organization members</option>
                <option value="PUBLIC">
                  Public — signed-in pilot link viewers
                </option>
              </select>
            </label>
            {event && (
              <label className="check">
                <input
                  name="open"
                  type="checkbox"
                  defaultChecked={event.signupOpen}
                />{" "}
                Open volunteer signup
              </label>
            )}
          </div>
          {!event && (
            <p className="muted small">
              New events start with signup closed. Save, add staffing, then open
              signup when you’re ready.
            </p>
          )}
          <button className="primary">{busy ? "Saving…" : "Save event"}</button>
        </fieldset>
      </form>
      {event && (
        <>
          <div className="section-heading">
            <h2>Jobs & shifts</h2>
            <span className="muted small">
              Times are entered in {event.timezone}
            </span>
          </div>
          {assigned && (
            <p className="info">
              Staffing structure and event timing are locked while confirmed
              assignments exist. Remove assignments before changing them. You
              can still update event details or close signup.
            </p>
          )}
          {event.jobs.map((job) => (
            <section className="panel" key={job.id}>
              <form onSubmit={(e) => saveJob(e, job.id)}>
                <fieldset disabled={busy || assigned}>
                  <legend>Edit job</legend>
                  <label>
                    Job name
                    <input
                      name="name"
                      maxLength={120}
                      required
                      defaultValue={job.name}
                    />
                  </label>
                  <label>
                    Job description
                    <textarea
                      name="description"
                      maxLength={1000}
                      defaultValue={job.description}
                    />
                  </label>
                  <label>
                    Eligibility notes
                    <textarea
                      name="eligibility"
                      maxLength={1000}
                      defaultValue={job.eligibility}
                    />
                  </label>
                  <button>Save job</button>
                </fieldset>
              </form>
              {job.shifts.map((s) => shiftForm(job.id, s))}
              {shiftForm(job.id, null)}
            </section>
          ))}
          <form className="panel" onSubmit={(e) => saveJob(e, null)}>
            <fieldset disabled={busy || assigned}>
              <legend>Add a job</legend>
              <label>
                Job name
                <input
                  name="name"
                  maxLength={120}
                  required
                  placeholder="e.g. Concessions"
                />
              </label>
              <label>
                Job description
                <textarea name="description" maxLength={1000} />
              </label>
              <label>
                Eligibility notes
                <textarea name="eligibility" maxLength={1000} />
              </label>
              <button className="primary">Add job</button>
            </fieldset>
          </form>
        </>
      )}
    </>
  );
}
