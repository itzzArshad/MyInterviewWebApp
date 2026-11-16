import type { MetadataRoute } from "next";

/** Web app manifest: lets the site be installed (iPhone "Add to Home Screen", desktop Chrome/Edge install). */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Backend Academy",
    short_name: "Academy",
    description: "Personal Java backend learning and interview preparation",
    start_url: "/dashboard",
    scope: "/",
    display: "standalone",
    orientation: "portrait-primary",
    background_color: "#191919",
    theme_color: "#191919",
    icons: [
      { src: "/icon/192", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icon/512", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icon/512", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
