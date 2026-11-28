import { useState, type FormEvent } from "react";
import { client, message } from "../api";
import { useSession, Alert, useLoad, Loader, LoadError } from "../ui";
import * as G from "../generated";
export function Settings() {
  const { organization } = useSession();
  const load = useLoad(() => client.request(G.TokensDocument), []);
  const [error, setError] = useState("");
  const [token, setToken] = useState("");
  const [busy, setBusy] = useState(false);
  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    setToken("");
    try {
      const result = await client.request(G.CreateTokenDocument, {
        organizationId: organization.organizationId,
        name: String(f.get("name")),
        writeEnabled: f.get("write") === "on",
      });
      setToken(result.createAgentToken.token);
      load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function revoke(id: string) {
    setBusy(true);
    try {
      await client.request(G.RevokeDocument, { id });
      load.refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">Your tools, your permissions</p>
          <h1>Agent access</h1>
          <p className="muted">
            Scoped access to the same GraphQL API, without copying browser
            sessions.
          </p>
        </div>
      </div>
      {error && <Alert>{error}</Alert>}
      {token && (
        <section className="panel">
          <h2>Copy your token now</h2>
          <p>
            Shown only once. Store it privately; it expires in seven days. Never
            paste it into shared conversations or source files.
          </p>
          <textarea aria-label="New agent token" readOnly value={token} />
          <button onClick={() => setToken("")}>Dismiss token</button>
        </section>
      )}
      <form className="panel" onSubmit={create}>
        <label>
          Token name
          <input name="name" maxLength={80} required />
        </label>
        <label className="check">
          <input type="checkbox" name="write" /> Allow changes within my
          existing permissions
        </label>
        <p className="small muted">
          Limited to {organization.organizationName}. A token cannot grant
          organizer access you do not already have.
        </p>
        <button className="primary" disabled={busy}>
          Create token
        </button>
      </form>
      {load.loading ? (
        <Loader />
      ) : load.error ? (
        <LoadError error={load.error} retry={load.refresh} />
      ) : (
        load.data?.agentTokens.map((t) => (
          <div className="registration-card" key={t.id}>
            <div>
              <strong>{t.name}</strong>
              <p className="small muted">
                {t.writeEnabled ? "Read & write" : "Read only"} · Expires{" "}
                {new Date(t.expiresAt).toLocaleDateString()} ·{" "}
                {t.revoked ? "Revoked" : "Active until expiry"}
              </p>
            </div>
            <button disabled={busy || t.revoked} onClick={() => revoke(t.id)}>
              Revoke
            </button>
          </div>
        ))
      )}
    </>
  );
}
