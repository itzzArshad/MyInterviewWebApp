"use client";

import { useEffect, useState } from "react";
import clsx from "clsx";
import { Check, ClipboardCopy, Loader2 } from "lucide-react";
import { api } from "@/lib/api";

/**
 * Copies the master prompt for this topic. The prompt is fetched up-front so the click handler
 * can write to the clipboard synchronously — iOS Safari rejects clipboard writes after an await.
 */
export function CopyPromptButton({ slug, className, label = "Copy prompt" }: { slug: string; className?: string; label?: string }) {
  const [prompt, setPrompt] = useState<string | null>(null);
  const [state, setState] = useState<"idle" | "copied" | "failed">("idle");
  const [show, setShow] = useState(false);

  useEffect(() => {
    api<{ prompt: string }>(`/lessons/${slug}/prompt`).then((p) => setPrompt(p.prompt)).catch(() => setState("failed"));
  }, [slug]);

  async function copy() {
    if (!prompt) return;
    try {
      await navigator.clipboard.writeText(prompt);
      setState("copied");
      setTimeout(() => setState("idle"), 2000);
    } catch {
      setState("failed");
      setShow(true); // fall back to manual select & copy
    }
  }

  return (
    <div className={className}>
      <div className="flex flex-wrap items-center gap-2">
        <button
          type="button"
          onClick={copy}
          disabled={!prompt}
          className={clsx(
            "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition disabled:opacity-60",
            state === "copied" ? "border-success/40 bg-success/10 text-success" : "border-border hover:bg-bg-hover",
          )}
        >
          {!prompt && state !== "failed" ? (
            <Loader2 className="size-4 animate-spin" />
          ) : state === "copied" ? (
            <Check className="size-4" />
          ) : (
            <ClipboardCopy className="size-4" />
          )}
          {state === "copied" ? "Prompt copied" : label}
        </button>
        <button type="button" onClick={() => setShow((s) => !s)} className="text-xs text-fg-faint hover:text-fg">
          {show ? "Hide prompt" : "View prompt"}
        </button>
      </div>
      {show && prompt && (
        <textarea
          readOnly
          value={prompt}
          onFocus={(e) => e.currentTarget.select()}
          rows={10}
          className="mt-2 w-full rounded-lg border border-border bg-bg-subtle p-3 font-mono text-xs"
        />
      )}
    </div>
  );
}
