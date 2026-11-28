import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { client, message } from "../api";
import { useLoad, Alert, Loader } from "../ui";
import * as G from "../generated";
export function Invite({ onAccepted }: { onAccepted: (id: string) => void }) {
  const { token = "" } = useParams();
  const load = useLoad(
    () => client.request(G.InvitationDocument, { token }),
    [token],
  );
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function accept() {
    setBusy(true);
    setError("");
    try {
      const result = await client.request(G.AcceptInvitationDocument, {
        token,
      });
      onAccepted(result.acceptInvitation.id);
    } catch (e) {
      setError(message(e));
      load.refresh();
    } finally {
      setBusy(false);
    }
  }
  if (load.loading) return <Loader />;
  if (load.error || !load.data)
    return (
      <>
        <Alert>
          This invitation is unavailable. It may have expired, been revoked, or
          already been used by another account.
        </Alert>
        <Link to="/">Back to workspace</Link>
      </>
    );
  const invitation = load.data.invitation;
  return (
    <>
      <div className="page-heading">
        <h1>Organization invitation</h1>
      </div>
      <section className="panel">
        <h2>{invitation.organizationName}</h2>
        <p>
          {invitation.canManage
            ? "Manage this organization and its members and events."
            : invitation.canOrganize
              ? "Create and manage all events in this organization."
              : invitation.eventId
                ? "Join this organization and manage the invited event."
                : "Join this organization as a volunteer."}
        </p>
        <p className="small muted">
          Expires {new Date(invitation.expiresAt).toLocaleString()}. Accepting
          adds these permissions to your account.
        </p>
        {error && <Alert>{error}</Alert>}
        <button className="primary" disabled={busy} onClick={accept}>
          {busy ? "Joining…" : "Accept invitation"}
        </button>
      </section>
    </>
  );
}
