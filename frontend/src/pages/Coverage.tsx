import { useState, type FormEvent } from "react";
import { client, message } from "../api";
import { Alert, useLoad, Loader, LoadError, localDate } from "../ui";
import * as G from "../generated";
export function Coverage({
  event,
  onChanged,
}: {
  event: G.EventFieldsFragment;
  onChanged: () => void;
}) {
  const [offset, setOffset] = useState(0);
  const load = useLoad(
    () => client.request(G.CoverageDocument, { eventId: event.id, offset }),
    [event.id, offset, event.revision],
  );
  const people = useLoad(
    () =>
      client.request(G.MembersDocument, {
        organizationId: event.organizationId,
      }),
    [event.organizationId],
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function assign(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      await client.request(G.AssignDocument, {
        shiftId: String(data.get("shift")),
        userId: String(data.get("person")),
        requestId: crypto.randomUUID(),
      });
      onChanged();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function remove(id: string) {
    if (!confirm("Remove this assignment?")) return;
    setBusy(true);
    try {
      await client.request(G.RemoveDocument, { id });
      onChanged();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="coverage">
      <div className="section-heading">
        <h2>Organizer coverage</h2>
        <button onClick={load.refresh}>Refresh coverage</button>
      </div>
      {error && <Alert>{error}</Alert>}
      {load.loading ? (
        <Loader />
      ) : load.error ? (
        <LoadError error={load.error} retry={load.refresh} />
      ) : (
        <>
          {load.data?.coverage.length === 0 ? (
            <p className="muted">No confirmed assignments yet.</p>
          ) : (
            load.data?.coverage.map((r) => (
              <div className="shift-row" key={r.id}>
                <div>
                  <strong>{r.participantName}</strong>
                  <span>
                    {event.jobs.find((j) => j.id === r.shift.jobId)?.name} ·{" "}
                    {localDate(r.shift.startsAt, event.timezone)}
                  </span>
                </div>
                <button disabled={busy} onClick={() => remove(r.id)}>
                  Remove assignment
                </button>
              </div>
            ))
          )}
          <div className="pagination">
            <button
              disabled={!offset}
              onClick={() => setOffset(Math.max(0, offset - 50))}
            >
              Previous assignments
            </button>
            <button
              disabled={(load.data?.coverage.length ?? 0) < 50}
              onClick={() => setOffset(offset + 50)}
            >
              Next assignments
            </button>
          </div>
        </>
      )}
      {people.error && <Alert>{people.error}</Alert>}
      <form className="assign-form" onSubmit={assign}>
        <h3>Assign a volunteer</h3>
        <label>
          Volunteer
          <select name="person" required>
            <option value="">Choose a member</option>
            {people.data?.members.map((p) => (
              <option key={p.id} value={p.id}>
                {p.displayName}
              </option>
            ))}
          </select>
        </label>
        <label>
          Shift
          <select name="shift" required>
            <option value="">Choose a shift</option>
            {event.jobs.flatMap((j) =>
              j.shifts.map((s) => (
                <option key={s.id} value={s.id}>
                  {j.name} · {localDate(s.startsAt, event.timezone)}
                </option>
              )),
            )}
          </select>
        </label>
        <button disabled={busy || !event.signupOpen || people.loading}>
          Assign volunteer
        </button>
        <p className="small muted">
          Assignments follow the same capacity and overlap rules as signup.
        </p>
      </form>
    </section>
  );
}
