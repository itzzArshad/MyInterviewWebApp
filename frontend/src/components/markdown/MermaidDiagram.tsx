"use client";

import { useEffect, useId, useState } from "react";
import { useTheme } from "next-themes";

/** Renders a ```mermaid block as an SVG diagram. Mermaid is loaded lazily, only on pages that need it. */
export function MermaidDiagram({ code }: { code: string }) {
  const id = useId().replace(/[^a-zA-Z0-9]/g, "");
  const { resolvedTheme } = useTheme();
  const [svg, setSvg] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const mermaid = (await import("mermaid")).default;
        mermaid.initialize({
          startOnLoad: false,
          securityLevel: "strict",
          theme: resolvedTheme === "dark" ? "dark" : "neutral",
          fontFamily: "inherit",
        });
        const { svg } = await mermaid.render(`m${id}${resolvedTheme ?? ""}`, code);
        if (!cancelled) {
          setSvg(svg);
          setError(null);
        }
      } catch (e) {
        if (!cancelled) setError((e as Error).message ?? "Invalid diagram");
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [code, id, resolvedTheme]);

  if (error) {
    return (
      <div className="rounded-lg border border-warning/40 bg-warning/5 p-3 text-sm">
        <p className="font-medium text-warning">Diagram could not be rendered</p>
        <pre className="mt-2 overflow-x-auto font-mono text-xs whitespace-pre-wrap text-fg-muted">{code}</pre>
      </div>
    );
  }
  return (
    <div
      className="mermaid-diagram flex justify-center overflow-x-auto rounded-lg border border-border bg-bg-subtle p-4"
      // Mermaid output with securityLevel "strict" (sanitised); source is the user's own lesson.
      dangerouslySetInnerHTML={svg ? { __html: svg } : undefined}
    >
      {svg ? undefined : <span className="text-sm text-fg-faint">Rendering diagram…</span>}
    </div>
  );
}
