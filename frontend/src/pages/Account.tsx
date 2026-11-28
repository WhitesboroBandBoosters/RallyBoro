import { useState, type FormEvent } from "react";
import { client, csrf, message } from "../api";
import { Alert } from "../ui";
import * as G from "../generated";
export function Account({ onChanged }: { onChanged: () => void }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [recoveryKey, setRecoveryKey] = useState("");
  async function change(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      await client.request(G.ChangePasswordDocument, {
        currentPassword: String(f.get("current")),
        newPassword: String(f.get("new")),
      });
      const token = await csrf();
      await fetch("/auth/logout", {
        method: "POST",
        headers: { [token.headerName]: token.token },
      });
      onChanged();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  async function rotate(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const f = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      const result = await client.request(G.RotateRecoveryKeyDocument, {
        currentPassword: String(f.get("current")),
      });
      setRecoveryKey(result.rotateRecoveryKey);
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <h1>Account</h1>
      </div>
      {error && <Alert>{error}</Alert>}
      <form className="panel" onSubmit={change}>
        <h2>Change password</h2>
        <label>
          Current password
          <input
            name="current"
            type="password"
            autoComplete="current-password"
            required
          />
        </label>
        <label>
          New password
          <input
            name="new"
            type="password"
            autoComplete="new-password"
            minLength={15}
            maxLength={72}
            required
          />
        </label>
        <p className="small muted">
          Use at least 15 characters, up to 72 UTF-8 bytes. Changing your
          password signs out existing local sessions and revokes your agent
          tokens. Your saved recovery key stays valid.
        </p>
        <button className="primary" disabled={busy}>
          {busy ? "Saving…" : "Change password and sign out"}
        </button>
      </form>
      <form className="panel" onSubmit={rotate}>
        <h2>Replace recovery key</h2>
        <p>
          Use this if you lost your saved key or haven’t saved one yet. The
          previous key will stop working.
        </p>
        <label>
          Password to replace recovery key
          <input
            name="current"
            type="password"
            autoComplete="current-password"
            required
          />
        </label>
        <button disabled={busy}>Replace recovery key</button>
      </form>
      {recoveryKey && (
        <section className="panel">
          <h2>Save your recovery key</h2>
          <p>Shown once. Store it privately before leaving this page.</p>
          <textarea
            aria-label="New recovery key"
            readOnly
            value={recoveryKey}
          />
          <button onClick={() => setRecoveryKey("")}>
            Dismiss recovery key
          </button>
        </section>
      )}
    </>
  );
}
