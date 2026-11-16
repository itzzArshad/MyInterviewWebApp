/** The app's monogram, drawn for next/og ImageResponse (inline styles only). */
export function AppMark({ size, rounded = true }: { size: number; rounded?: boolean }) {
  return (
    <div
      style={{
        width: size,
        height: size,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        background: "#191919",
        borderRadius: rounded && size <= 64 ? size * 0.22 : 0,
        color: "#ffffff",
        fontSize: size * 0.4,
        fontWeight: 700,
        letterSpacing: -size * 0.02,
        fontFamily: "sans-serif",
      }}
    >
      <span style={{ display: "flex" }}>
        B<span style={{ color: "#529cca" }}>A</span>
      </span>
    </div>
  );
}
