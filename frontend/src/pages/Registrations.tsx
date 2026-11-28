import { useState } from "react";
import { Link } from "react-router-dom";
import { client, message } from "../api";
import { Alert, Empty, useLoad, Loader, LoadError, localDate } from "../ui";
import * as G from "../generated";
export function Registrations() {
  const [offset, setOffset] = useState(0);
  const load = useLoad(
    () => client.request(G.RegistrationsDocument, { offset }),
    [offset],
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  async function cancel(id: string) {
    if (!confirm("Cancel this registration and free your place?")) return;
    setBusy(id);
    setError("");
    try {
      await client.request(G.CancelDocument, { id });
      load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy("");
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Your time makes a difference</p>
          <h1>My registrations</h1>
          <p className="muted">Your commitments, in one place.</p>
        </div>
      </div>
      {error && <Alert>{error}</Alert>}
      {load.loading ? (
        <Loader />
      ) : load.error ? (
        <LoadError error={load.error} retry={load.refresh} />
      ) : (
        load.data && (
          <>
            {load.data.myRegistrations.length === 0 ? (
              <Empty title="Your next good thing is waiting">
                <Link to="/">Explore your organization’s events</Link>
              </Empty>
            ) : (
              load.data.myRegistrations.map((r) => (
                <article className="registration-card" key={r.id}>
                  <div>
                    <span
                      className={
                        "badge " +
                        (r.status === "CONFIRMED" ? "open" : "closed")
                      }
                    >
                      {r.status === "CONFIRMED" ? "Confirmed" : "Cancelled"}
                    </span>
                    <h2>
                      <Link to={"/events/" + r.event.id}>{r.event.title}</Link>
                    </h2>
                    <strong>
                      {r.event.jobs.find((j) => j.id === r.shift.jobId)?.name}
                    </strong>
                    <p>
                      {localDate(r.shift.startsAt, r.event.timezone)} ·{" "}
                      {r.event.timezone}
                    </p>
                  </div>
                  {r.status === "CONFIRMED" && (
                    <button disabled={!!busy} onClick={() => cancel(r.id)}>
                      {busy === r.id ? "Cancelling…" : "Cancel registration"}
                    </button>
                  )}
                </article>
              ))
            )}
            <div className="pagination">
              <button
                disabled={!offset}
                onClick={() => setOffset(Math.max(0, offset - 25))}
              >
                Previous
              </button>
              <button
                disabled={load.data.myRegistrations.length < 25}
                onClick={() => setOffset(offset + 25)}
              >
                Next
              </button>
            </div>
          </>
        )
      )}
    </>
  );
}
