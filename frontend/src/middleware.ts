import { NextResponse, type NextRequest } from "next/server";

export const AUTH_COOKIE = "ACADEMY_TOKEN";

/**
 * Fast UX redirect only: real authorization is enforced by the backend on every API call
 * (an expired/forged cookie gets a 401 and the client sends you back to /login).
 */
export function middleware(req: NextRequest) {
  const hasSession = Boolean(req.cookies.get(AUTH_COOKIE)?.value);
  const { pathname } = req.nextUrl;
  const isLogin = pathname === "/login";

  if (!hasSession && !isLogin) {
    const url = req.nextUrl.clone();
    url.pathname = "/login";
    url.search = pathname === "/" ? "" : `?next=${encodeURIComponent(pathname)}`;
    return NextResponse.redirect(url);
  }
  if (hasSession && (isLogin || pathname === "/")) {
    const url = req.nextUrl.clone();
    url.pathname = "/dashboard";
    url.search = "";
    return NextResponse.redirect(url);
  }
  return NextResponse.next();
}

export const config = {
  // Skip API proxy, Next internals and static files.
  // Install assets (manifest, icons) must load without a session.
  matcher: ["/((?!api|_next/static|_next/image|favicon.ico|icon|apple-icon|manifest.webmanifest|.*\\.(?:png|svg|ico|webmanifest)$).*)"],
};
