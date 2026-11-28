import { useState, useEffect, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { client, csrf, message } from "../api";
import { Alert } from "../ui";
import * as G from "../generated";
export function Login({
  onLogin,
  sessionError,
}: {
  onLogin: () => void;
  sessionError?: string;
}) {
  const [options, setOptions] = useState<{ local: boolean; oidc: boolean }>();
  const [mode, setMode] = useState<"login" | "register" | "recover">("login");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [key, setKey] = useState("");
  const [saved, setSaved] = useState(false);
  const [username, setUsername] = useState("");
  useEffect(() => {
    fetch("/auth/options")
      .then((r) => r.json())
      .then(setOptions)
      .catch(() => setError("Could not load sign-in options."));
  }, []);
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const data = new FormData(e.currentTarget);
    const name = String(data.get("username"));
    setUsername(name);
    setBusy(true);
    setError("");
    try {
      if (mode === "register") {
        const result = await client.request(G.RegisterAccountDocument, {
          username: name,
          displayName: String(data.get("displayName")),
          password: String(data.get("password")),
        });
        setKey(result.registerAccount.recoveryKey);
        setSaved(false);
      } else if (mode === "recover") {
        const result = await client.request(G.RecoverAccountDocument, {
          username: name,
          recoveryKey: String(data.get("recoveryKey")),
          newPassword: String(data.get("password")),
        });
        setKey(result.recoverAccount);
        setSaved(false);
      } else {
        const token = await csrf();
        const response = await fetch("/auth/login", {
          method: "POST",
          headers: { [token.headerName]: token.token },
          body: new URLSearchParams({
            username: name,
            password: String(data.get("password")),
          }),
        });
        if (!response.ok)
          throw new Error("Sign-in failed. Check your username and password.");
        onLogin();
      }
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  function choose(next: typeof mode) {
    setMode(next);
    setError("");
  }
  return (
    <main className="login">
      <section className="login-panel">
        <Link className="brand" to="/">
          RallyBoro
        </Link>
        <h1>
          {key
            ? "Save your recovery key"
            : mode === "register"
              ? "Create account"
              : mode === "recover"
                ? "Recover account"
                : "Sign in"}
        </h1>
        {(error ||
          (mode === "login" &&
            sessionError &&
            !sessionError.includes("Please sign in"))) && (
          <Alert>{error || sessionError}</Alert>
        )}
        {key ? (
          <>
            <p>
              This key is shown once. Store it privately so you can reset your
              password without email.{" "}
              {mode === "recover" && "Your previous key no longer works."}
            </p>
            <textarea aria-label="Recovery key" readOnly value={key} />
            <label className="check">
              <input
                type="checkbox"
                checked={saved}
                onChange={(e) => setSaved(e.target.checked)}
              />
              I saved my recovery key
            </label>
            <button
              className="primary"
              disabled={!saved}
              onClick={() => {
                setKey("");
                choose("login");
              }}
            >
              Continue to sign in
            </button>
          </>
        ) : (
          <>
            {mode === "login" && options?.oidc && (
              <a className="button primary" href="/oauth2/authorization/google">
                Continue with Google
              </a>
            )}
            {options?.local && (
              <>
                <form key={mode} onSubmit={submit}>
                  {mode === "register" && (
                    <label>
                      Display name
                      <input
                        name="displayName"
                        autoComplete="name"
                        required
                        maxLength={80}
                      />
                    </label>
                  )}
                  <label>
                    Username
                    <input
                      name="username"
                      autoComplete="username"
                      required
                      minLength={mode === "login" ? 1 : 3}
                      maxLength={40}
                      pattern="[a-zA-Z0-9_-]+"
                      defaultValue={username}
                    />
                  </label>
                  {mode === "recover" && (
                    <label>
                      Recovery key
                      <input
                        name="recoveryKey"
                        autoComplete="off"
                        required
                        maxLength={100}
                      />
                    </label>
                  )}
                  <label>
                    {mode === "recover" ? "New password" : "Password"}
                    <input
                      name="password"
                      type="password"
                      autoComplete={
                        mode === "login" ? "current-password" : "new-password"
                      }
                      required
                      minLength={mode === "login" ? undefined : 15}
                      maxLength={mode === "login" ? undefined : 72}
                    />
                  </label>
                  {mode !== "login" && (
                    <p className="small muted">
                      Use at least 15 characters, up to 72 UTF-8 bytes. No email
                      address is required.
                    </p>
                  )}
                  <button className="primary" disabled={busy}>
                    {busy
                      ? "Please wait…"
                      : mode === "register"
                        ? "Create account"
                        : mode === "recover"
                          ? "Reset password"
                          : "Sign in"}
                  </button>
                </form>
                <div className="login-actions">
                  {mode !== "login" && (
                    <button onClick={() => choose("login")}>
                      Back to sign in
                    </button>
                  )}
                  {mode === "login" && (
                    <>
                      <button onClick={() => choose("register")}>
                        Create account
                      </button>
                      <button onClick={() => choose("recover")}>
                        Recover account
                      </button>
                    </>
                  )}
                </div>
              </>
            )}
            {options && !options.local && !options.oidc && (
              <Alert>
                Sign-in has not been configured by the deployment operator.
              </Alert>
            )}
          </>
        )}
      </section>
    </main>
  );
}
