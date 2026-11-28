import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { client, message } from "../api";
import {
  Alert,
  Empty,
  useLoad,
  Loader,
  LoadError,
  localDate,
  dateLabel,
  Badge,
} from "../ui";
import * as G from "../generated";
import { Coverage } from "./Coverage";
export function EventView() {
  const { id = "" } = useParams();
  const load = useLoad(() => client.request(G.EventDocument, { id }), [id]);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState("");
  async function signup(shiftId: string) {
    setBusy(shiftId);
    setError("");
    setNotice("");
    try {
      await client.request(G.SignupDocument, {
        shiftId,
        requestId: crypto.randomUUID(),
      });
      setNotice("You’re confirmed! Find this shift in My registrations.");
      load.refresh();
    } catch (e) {
      setError(message(e));
      load.refresh();
    } finally {
      setBusy("");
    }
  }
  if (load.loading) return <Loader />;
  if (load.error || !load.data)
    return (
      <LoadError
        error={load.error ?? "Event unavailable"}
        retry={load.refresh}
      />
    );
  const event = load.data.event;
  const shifts = event.jobs.flatMap((j) => j.shifts);
  const cap = shifts.reduce((a, s) => a + s.capacity, 0),
    filled = shifts.reduce((a, s) => a + s.filled, 0);
  return (
    <>
      <Link className="back" to="/">
        ← All events
      </Link>
      <div className="page-heading">
        <div>
          <Badge event={event} />
          <h1>{event.title}</h1>
          <p className="muted">
            {dateLabel(event.eventDate)} ·{" "}
            {event.location || "Location to be confirmed"}
          </p>
          <p className="small">Times shown in {event.timezone}</p>
        </div>
        {event.canManage && (
          <Link className="button" to={"/events/" + id + "/edit"}>
            Edit event
          </Link>
        )}
      </div>
      <p className="event-intro">{event.description}</p>
      <div className="stats">
        <div>
          <strong>{event.jobs.length}</strong>
          <span>Jobs</span>
        </div>
        <div>
          <strong>
            {filled}/{cap}
          </strong>
          <span>Places filled</span>
        </div>
        <div>
          <strong>{cap - filled}</strong>
          <span>Places available</span>
        </div>
      </div>
      {error && <Alert>{error}</Alert>}
      {notice && (
        <p className="success" role="status">
          {notice}
        </p>
      )}
      <div className="section-heading">
        <h2>Shifts</h2>
        <button onClick={load.refresh}>Refresh availability</button>
      </div>
      {!event.signupOpen && (
        <p className="info">
          Signup is currently closed. Your existing commitments remain in My
          registrations.
        </p>
      )}
      {event.jobs.length === 0 ? (
        <Empty title="No shifts yet">
          {event.canManage ? (
            <Link to={"/events/" + id + "/edit"}>
              Add your first job and shift
            </Link>
          ) : (
            "No shifts have been added."
          )}
        </Empty>
      ) : (
        event.jobs.map((job) => (
          <section className="job-panel" key={job.id}>
            <h3>{job.name}</h3>
            <p>{job.description}</p>
            {job.eligibility && (
              <p className="small muted">Please note: {job.eligibility}</p>
            )}
            {job.shifts.map((shift) => (
              <div className="shift-row" key={shift.id}>
                <div>
                  <strong>
                    {localDate(shift.startsAt, event.timezone)} –{" "}
                    {new Intl.DateTimeFormat(undefined, {
                      hour: "numeric",
                      minute: "2-digit",
                      timeZone: event.timezone,
                    }).format(new Date(shift.endsAt))}
                  </strong>
                  <span className="small muted">
                    {shift.filled} of {shift.capacity} filled ·{" "}
                    {shift.remaining} available
                  </span>
                </div>
                <button
                  className="primary"
                  disabled={
                    !event.signupOpen || shift.remaining === 0 || !!busy
                  }
                  onClick={() => signup(shift.id)}
                >
                  {busy === shift.id
                    ? "Confirming…"
                    : shift.remaining === 0
                      ? "Full"
                      : "Sign up"}
                </button>
              </div>
            ))}
            {job.shifts.length === 0 && (
              <p className="muted">Shifts have not been added yet.</p>
            )}
          </section>
        ))
      )}
      {event.canManage && <Coverage event={event} onChanged={load.refresh} />}
    </>
  );
}
