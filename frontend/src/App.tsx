import { useState } from "react";
import {
  Link,
  NavLink,
  Navigate,
  Route,
  Routes,
  useNavigate,
} from "react-router-dom";
import { client, csrf } from "./api";
import * as G from "./generated";
import { Session, useLoad, Loader, Empty } from "./ui";
import { EventList } from "./pages/EventList";
import { EventView } from "./pages/EventView";
import { Registrations } from "./pages/Registrations";
import { EventEditor } from "./pages/EventEditor";
import { Settings } from "./pages/Settings";
import { Login } from "./pages/Login";
import { CreateOrganization } from "./pages/Onboarding";
import { Invite } from "./pages/Invite";
import { Organization } from "./pages/Organization";
import { Account } from "./pages/Account";
export function App() {
  const navigate = useNavigate();
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
  async function logout() {
    const token = await csrf();
    await fetch("/auth/logout", {
      method: "POST",
      headers: { [token.headerName]: token.token },
    });
    bump((v) => v + 1);
  }
  function joined(id: string) {
    setSelected(id);
    navigate("/", { replace: true });
    bump((v) => v + 1);
  }
  if (!organization)
    return (
      <main className="onboarding content">
        <div className="onboarding-header">
          <Link className="brand" to="/">
            RallyBoro
          </Link>
          <div className="action-row">
            <Link className="button" to="/my-registrations">
              My registrations
            </Link>
            <Link className="button" to="/account">
              Account
            </Link>
            <button onClick={logout}>Sign out</button>
          </div>
        </div>
        <Routes>
          <Route path="/events/:id" element={<EventView />} />
          <Route path="/my-registrations" element={<Registrations />} />
          <Route
            path="/invite/:token"
            element={<Invite onAccepted={joined} />}
          />
          <Route
            path="/account"
            element={<Account onChanged={() => bump((v) => v + 1)} />}
          />
          <Route
            path="*"
            element={
              <>
                <CreateOrganization onCreated={joined} />
                <p className="muted">
                  To join an existing organization, open the invitation link
                  shared with you.
                </p>
              </>
            }
          />
        </Routes>
      </main>
    );
  return (
    <Session.Provider
      value={{
        viewer,
        organization,
        reload: (id) => {
          if (id) setSelected(id);
          bump((v) => v + 1);
        },
      }}
    >
      <div className="workspace">
        <aside className="sidebar">
          <Link className="brand" to="/">
            RallyBoro
          </Link>
          {viewer.memberships.length > 1 ? (
            <label className="org-picker">
              Organization
              <select
                aria-label="Organization"
                value={organization.organizationId}
                onChange={(e) => {
                  setSelected(e.target.value);
                  navigate("/");
                }}
              >
                {viewer.memberships.map((m) => (
                  <option key={m.organizationId} value={m.organizationId}>
                    {m.organizationName}
                  </option>
                ))}
              </select>
            </label>
          ) : (
            <p className="organization-name">{organization.organizationName}</p>
          )}
          <nav aria-label="Main navigation">
            <NavLink to="/" end>
              <span aria-hidden="true">▦</span> Events
            </NavLink>
            <NavLink to="/my-registrations">
              <span aria-hidden="true">✓</span> My registrations
            </NavLink>
            {organization.canManage && (
              <NavLink to="/organizations">Organization</NavLink>
            )}
          </nav>
          <details className="account-menu">
            <summary>Account</summary>
            <div className="account-panel">
              <strong>{viewer.displayName}</strong>
              <small>
                {organization.canOrganize ? "Event organizer" : "Volunteer"}
              </small>
              <Link
                to="/organizations/new"
                onClick={(e) =>
                  e.currentTarget.closest("details")?.removeAttribute("open")
                }
              >
                Create organization
              </Link>
              <Link
                to="/account"
                onClick={(e) =>
                  e.currentTarget.closest("details")?.removeAttribute("open")
                }
              >
                Account settings
              </Link>
              <Link
                to="/settings"
                onClick={(e) =>
                  e.currentTarget.closest("details")?.removeAttribute("open")
                }
              >
                Agent access
              </Link>
              <button onClick={logout}>Sign out</button>
            </div>
          </details>
        </aside>
        <main className="content">
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
              path="/account"
              element={<Account onChanged={() => bump((v) => v + 1)} />}
            />
            <Route
              path="/organizations"
              element={<Organization key={organization.organizationId} />}
            />
            <Route
              path="/organizations/new"
              element={<CreateOrganization onCreated={joined} />}
            />
            <Route
              path="/invite/:token"
              element={<Invite onAccepted={joined} />}
            />
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
