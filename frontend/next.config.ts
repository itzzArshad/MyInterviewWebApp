import path from "node:path";
import type { NextConfig } from "next";

/**
 * The browser only ever talks to this Next.js origin. `/api/*` is proxied to Spring Boot,
 * so the auth cookie is first-party (works on iOS Safari) and no CORS is needed.
 */
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const securityHeaders = [
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "X-Frame-Options", value: "DENY" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  poweredByHeader: false,
  // The repo root has its own package-lock.json (Neon tooling); this app's root is frontend/.
  outputFileTracingRoot: path.join(__dirname),
  async rewrites() {
    return [{ source: "/api/:path*", destination: `${backendUrl}/api/:path*` }];
  },
  async headers() {
    return [{ source: "/:path*", headers: securityHeaders }];
  },
};

export default nextConfig;
