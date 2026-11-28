import { message } from "./api";
import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import * as G from "./generated";
type Viewer = G.ViewerQuery["viewer"];
type Membership = Viewer["memberships"][number];
export const Session = createContext<{
  viewer: Viewer;
  organization: Membership;
  reload: () => void;
} | null>(null);
export function useSession() {
  const session = useContext(Session);
  if (!session) throw new Error("Session unavailable");
  return session;
}

export function Alert({ children }: { children: ReactNode }) {
  return (
    <div className="alert" role="alert">
      {children}
    </div>
  );
}

export function Empty({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="empty">
      <span className="empty-icon" aria-hidden="true">
        ✦
      </span>
      <h2>{title}</h2>
      <p>{children}</p>
    </section>
  );
}

export function useLoad<T>(loader: () => Promise<T>, deps: unknown[]) {
  const [state, set] = useState<{ data?: T; error?: string; loading: boolean }>(
    { loading: true },
  );
  const [version, bump] = useState(0);
  useEffect(() => {
    let live = true;
    set({ loading: true });
    loader()
      .then((data) => {
        if (live) set({ data, loading: false });
      })
      .catch((e) => {
        if (live) set({ error: message(e), loading: false });
      });
    return () => {
      live = false;
    };
  }, [...deps, version]);
  return {
    ...state,
    refresh: () => {
      set({ loading: true });
      bump((v) => v + 1);
    },
  };
}

export function Loader() {
  return (
    <p className="loading" role="status">
      Loading your workspace…
    </p>
  );
}

export function LoadError({
  error,
  retry,
}: {
  error: string;
  retry: () => void;
}) {
  return (
    <>
      <Alert>{error}</Alert>
      <button onClick={retry}>Try again</button>
    </>
  );
}

export function localDate(iso: string, zone: string) {
  return new Intl.DateTimeFormat(undefined, {
    timeZone: zone,
    month: "short",
    day: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(new Date(iso));
}

export function dateLabel(date: string) {
  return new Intl.DateTimeFormat(undefined, {
    weekday: "short",
    month: "short",
    day: "numeric",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(date + "T12:00:00Z"));
}

export function Badge({ event }: { event: G.EventFieldsFragment }) {
  return (
    <span className={"badge " + (event.signupOpen ? "open" : "closed")}>
      {event.visibility === "HIDDEN"
        ? "Draft"
        : event.signupOpen
          ? "Signup open"
          : "Signup closed"}
    </span>
  );
}
