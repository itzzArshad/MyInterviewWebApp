import { ImageResponse } from "next/og";
import { AppMark } from "./app-mark";

/** Favicon + install icons, generated at build time: /icon/32, /icon/192, /icon/512. */
export function generateImageMetadata() {
  return [32, 192, 512].map((s) => ({ id: String(s), size: { width: s, height: s }, contentType: "image/png" }));
}

export default async function Icon({ id }: { id: Promise<string> | string }) {
  const size = Number(await id);
  return new ImageResponse(<AppMark size={size} />, { width: size, height: size });
}
