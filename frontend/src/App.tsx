import { useState } from "react";
import { Link, NavLink, Navigate, Route, Routes } from "react-router-dom";
import { client, csrf } from "./api";
import * as G from "./generated";
import { Session, useLoad, Loader, Empty } from "./ui";
import { EventList } from "./pages/EventList";
import { EventView } from "./pages/EventView";
import { Registrations } from "./pages/Registrations";
import { EventEditor } from "./pages/EventEditor";
import { Settings } from "./pages/Settings";
import { Login } from "./pages/Login";
export function App() {
  const [version, bump] = useState(0);
  const [selected, setSelected] = useState("");
  const session = useLoad(() => client.request(G.ViewerDocument), [version]);
  if (session.loading) return <Loader />;
  if (session.error || !session.data)
    return (
      <Login onLogin={() => bump((v) => v + 1)} sessionError={session.error} />
    );
  const viewer = session.data.viewer;
  const organization =
    viewer.memberships.find((m) => m.organizationId === selected) ??
    viewer.memberships[0];
  if (!organization)
    return (
      <Empty title="You’re signed in">
        An organizer needs to enroll this account in an organization.
      </Empty>
    );
  async function logout() {
    const token = await csrf();
    await fetch("/auth/logout", {
      method: "POST",
      headers: { [token.headerName]: token.token },
    });
    bump((v) => v + 1);
  }
  return (
    <Session.Provider
      value={{ viewer, organization, reload: () => bump((v) => v + 1) }}
    >
      <div className="workspace">
        <aside className="sidebar">
          <Link className="brand" to="/">
            RallyBoro
          </Link>
          <label className="org-picker">
            Organization
            <select
              aria-label="Organization"
              value={organization.organizationId}
              onChange={(e) => setSelected(e.target.value)}
            >
              {viewer.memberships.map((m) => (
                <option key={m.organizationId} value={m.organizationId}>
                  {m.organizationName}
                </option>
              ))}
            </select>
          </label>
          <nav aria-label="Main navigation">
            <NavLink to="/" end>
              <span aria-hidden="true">▦</span> Events
            </NavLink>
            <NavLink to="/my-registrations">
              <span aria-hidden="true">✓</span> My registrations
            </NavLink>
            <NavLink to="/settings">
              <span aria-hidden="true">⚙</span> Agent access
            </NavLink>
          </nav>
          <div className="sidebar-foot">
            <span className="avatar">{viewer.displayName.slice(0, 1)}</span>
            <div>
              <strong>{viewer.displayName}</strong>
              <small>
                {organization.role === "ORGANIZER"
                  ? "Event organizer"
                  : "Volunteer"}
              </small>
            </div>
            <button className="text-button" onClick={logout}>
              Sign out
            </button>
          </div>
        </aside>
        <main className="content">
          <header className="topbar">
            <span>{organization.organizationName}</span>
            <span className="pilot-pill">Staffing pilot</span>
          </header>
          <Routes>
            <Route path="/login" element={<Navigate to="/" replace />} />
            <Route path="/" element={<EventList />} />
            <Route path="/events" element={<EventList />} />
            <Route path="/events/new" element={<EventEditor />} />
            <Route path="/events/:id" element={<EventView />} />
            <Route path="/events/:id/edit" element={<EventEditor />} />
            <Route path="/my-registrations" element={<Registrations />} />
            <Route path="/settings" element={<Settings />} />
            <Route
              path="*"
              element={
                <Empty title="Page not found">
                  <Link to="/">Return to events</Link>
                </Empty>
              }
            />
          </Routes>
        </main>
      </div>
    </Session.Provider>
  );
}
