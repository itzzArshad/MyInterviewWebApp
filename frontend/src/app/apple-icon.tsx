import { ImageResponse } from "next/og";
import { AppMark } from "./app-mark";

export const size = { width: 180, height: 180 };
export const contentType = "image/png";

/** iPhone home-screen icon (iOS adds its own rounded corners). */
export default function AppleIcon() {
  return new ImageResponse(<AppMark size={180} rounded={false} />, size);
}
