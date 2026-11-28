import { useState } from "react";
import { Link } from "react-router-dom";
import { client } from "../api";
import {
  useSession,
  Empty,
  useLoad,
  Loader,
  LoadError,
  dateLabel,
  Badge,
} from "../ui";
import * as G from "../generated";
export function EventList() {
  const { organization } = useSession();
  const [offset, setOffset] = useState(0);
  const load = useLoad(
    () =>
      client.request(G.EventsDocument, {
        organizationId: organization.organizationId,
        offset,
      }),
    [organization.organizationId, offset],
  );
  const organizer = organization.role === "ORGANIZER";
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>{organizer ? "Your events" : "Find your next shift"}</h1>
        </div>
        {organizer && (
          <Link className="button primary" to="/events/new">
            ＋ Create event
          </Link>
        )}
      </div>
      {load.loading ? (
        <Loader />
      ) : load.error ? (
        <LoadError error={load.error} retry={load.refresh} />
      ) : (
        load.data && (
          <>
            {load.data.events.items.length === 0 ? (
              <Empty
                title={organizer ? "No events yet" : "No events to show yet"}
              >
                {organizer
                  ? "Create an event and add jobs and shifts."
                  : "Check back when your organization opens its next event."}
              </Empty>
            ) : (
              <div className="event-grid">
                {load.data.events.items.map((event) => {
                  const shifts = event.jobs.flatMap((j) => j.shifts);
                  const capacity = shifts.reduce((a, s) => a + s.capacity, 0);
                  const filled = shifts.reduce((a, s) => a + s.filled, 0);
                  return (
                    <article className="event-card" key={event.id}>
                      <div className="card-top">
                        <span className="event-date">
                          {dateLabel(event.eventDate)}
                        </span>
                        <Badge event={event} />
                      </div>
                      <h2>
                        <Link to={"/events/" + event.id}>{event.title}</Link>
                      </h2>
                      <p className="location">
                        {event.location || "Location to be confirmed"}
                      </p>
                      <p className="card-description">
                        {event.description || "No description provided."}
                      </p>
                      <div className="coverage-label">
                        <span>
                          {filled} of {capacity} places filled
                        </span>
                        <strong>{capacity - filled} available</strong>
                      </div>
                      <progress
                        aria-label={`${event.title} staffing coverage`}
                        value={filled}
                        max={capacity || 1}
                      />
                      <div className="card-footer">
                        <span>
                          {event.jobs.length} jobs · {shifts.length} shifts
                        </span>
                        <Link to={"/events/" + event.id}>View event →</Link>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
            <div className="pagination">
              <button
                disabled={offset === 0}
                onClick={() => setOffset(Math.max(0, offset - 12))}
              >
                Previous
              </button>
              <span>{load.data.events.total} events</span>
              <button
                disabled={offset + 12 >= load.data.events.total}
                onClick={() => setOffset(offset + 12)}
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
