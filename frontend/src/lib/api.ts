export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    /** Parsed problem+json body, when the server sent one (e.g. `errors`, `warnings`). */
    public problem?: Record<string, unknown>,
  ) {
    super(message);
  }
}

/** JSON fetch against the same-origin `/api` proxy. Redirects to /login on 401. */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`/api${path}`, {
    ...init,
    credentials: "same-origin",
    headers: {
      Accept: "application/json",
      ...(init.body ? { "Content-Type": "application/json" } : {}),
      ...init.headers,
    },
  });

  if (res.status === 401 && path !== "/auth/login" && typeof window !== "undefined") {
    const next = encodeURIComponent(window.location.pathname);
    window.location.href = `/login?next=${next}`;
    throw new ApiError(401, "Session expired");
  }
  if (!res.ok) {
    let message = res.statusText || "Request failed";
    let problem: Record<string, unknown> | undefined;
    try {
      problem = await res.json();
      message = (problem?.detail as string) ?? (problem?.title as string) ?? message;
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, message, problem);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
