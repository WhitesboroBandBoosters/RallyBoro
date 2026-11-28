import { useState, useEffect, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { csrf, message } from "../api";
import { Alert } from "../ui";
export function Login({
  onLogin,
  sessionError,
}: {
  onLogin: () => void;
  sessionError?: string;
}) {
  const [options, setOptions] = useState<{ local: boolean; oidc: boolean }>();
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    fetch("/auth/options")
      .then((r) => r.json())
      .then(setOptions)
      .catch(() => setError("Could not load sign-in options."));
  }, []);
  async function login(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    setBusy(true);
    setError("");
    try {
      const token = await csrf();
      const response = await fetch("/auth/login", {
        method: "POST",
        headers: { [token.headerName]: token.token },
        body: new URLSearchParams({
          username: String(form.get("username")),
          password: String(form.get("password")),
        }),
      });
      if (!response.ok)
        throw new Error(
          "Sign-in failed. Check your credentials and pilot access.",
        );
      onLogin();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login">
      <section className="login-story">
        <Link className="brand" to="/">
          RallyBoro<span>●</span>
        </Link>
        <p className="eyebrow">A little time. A big difference.</p>
        <h1>
          Good things happen
          <br />
          when we show up.
        </h1>
        <p>
          A shared place for community events, volunteer shifts, and the people
          who make them happen.
        </p>
        <div className="story-line">
          Community-powered. One shift at a time.
        </div>
      </section>
      <section className="login-panel">
        <p className="eyebrow">Your volunteer workspace</p>
        <h2>Welcome to RallyBoro</h2>
        <p>Sign in to find your next shift or organize an event.</p>
        {(error ||
          (sessionError && !sessionError.includes("Please sign in"))) && (
          <Alert>{error || sessionError}</Alert>
        )}
        {options?.oidc && (
          <a className="button primary" href="/oauth2/authorization/google">
            Continue with Google
          </a>
        )}
        {options?.local && (
          <form onSubmit={login}>
            <label>
              Pilot username
              <input
                name="username"
                autoComplete="username"
                required
                maxLength={40}
              />
            </label>
            <label>
              Password
              <input
                name="password"
                type="password"
                autoComplete="current-password"
                required
              />
            </label>
            <button className="primary" disabled={busy}>
              {busy ? "Signing in…" : "Sign in"}
            </button>
            <p className="muted small">
              Controlled pilot · accounts are provided by your organizer.
            </p>
          </form>
        )}
        {options && !options.local && !options.oidc && (
          <Alert>
            Sign-in has not been configured by the deployment operator.
          </Alert>
        )}
      </section>
    </main>
  );
}
