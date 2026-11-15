import "server-only";
import { cookies } from "next/headers";
import { notFound, redirect } from "next/navigation";

const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";
const AUTH_COOKIE = "ACADEMY_TOKEN";

/**
 * Fetch from Spring Boot during server rendering, forwarding the user's auth cookie.
 * 401 → login page, 404 → Next.js not-found page.
 */
export async function serverApi<T>(path: string): Promise<T> {
  const token = (await cookies()).get(AUTH_COOKIE)?.value;
  if (!token) redirect("/login");

  const res = await fetch(`${BACKEND_URL}/api${path}`, {
    headers: { Accept: "application/json", Cookie: `${AUTH_COOKIE}=${token}` },
    cache: "no-store",
  });
  if (res.status === 401) redirect("/login");
  if (res.status === 404) notFound();
  if (!res.ok) throw new Error(`Backend error ${res.status} for ${path}`);
  return res.json() as Promise<T>;
}
