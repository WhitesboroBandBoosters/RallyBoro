import { GraphQLClient, ClientError } from "graphql-request";
export async function csrf(): Promise<{ headerName: string; token: string }> {
  const response = await fetch("/auth/csrf", {
    credentials: "same-origin",
    cache: "no-store",
  });
  if (!response.ok)
    throw new Error("Unable to establish a secure session. Please reload.");
  return response.json();
}
export const client = new GraphQLClient(
  new URL("/graphql", window.location.origin).href,
  {
    credentials: "same-origin",
    requestMiddleware: async (request) => {
      const token = await csrf();
      const headers = new Headers(request.headers);
      headers.set(token.headerName, token.token);
      return { ...request, headers };
    },
  },
);
export function message(error: unknown): string {
  if (error instanceof ClientError)
    return (
      error.response.errors?.map((e) => e.message).join(" ") ||
      "The request failed. Refresh and try again."
    );
  return error instanceof Error
    ? error.message
    : "Something went wrong. Please try again.";
}
