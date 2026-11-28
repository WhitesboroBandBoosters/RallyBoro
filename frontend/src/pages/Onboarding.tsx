import { useState, type FormEvent } from "react";
import { client, message } from "../api";
import { Alert } from "../ui";
import * as G from "../generated";
export function CreateOrganization({
  onCreated,
}: {
  onCreated: (id: string) => void;
}) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      const result = await client.request(G.CreateOrganizationDocument, {
        name: String(data.get("name")),
      });
      onCreated(result.createOrganization.id);
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-heading">
        <h1>Create organization</h1>
      </div>
      <form className="panel" onSubmit={create}>
        {error && <Alert>{error}</Alert>}
        <label>
          Organization name
          <input name="name" required maxLength={120} />
        </label>
        <p className="small muted">
          You can manage this organization and invite others. This does not
          change your access to other organizations.
        </p>
        <button className="primary" disabled={busy}>
          {busy ? "Creating…" : "Create organization"}
        </button>
      </form>
    </>
  );
}
