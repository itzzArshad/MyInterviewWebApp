"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { Bookmark, CheckCircle2, Circle, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { ProgressView } from "@/lib/types";

/** Records the visit, and lets you bookmark / complete the lesson. */
export function LessonActions({ slug, initial }: { slug: string; initial: ProgressView }) {
  const router = useRouter();
  const [progress, setProgress] = useState(initial);
  const [busy, setBusy] = useState<"complete" | "bookmark" | null>(null);
  const viewed = useRef(false);

  useEffect(() => {
    if (viewed.current) return;
    viewed.current = true;
    api<ProgressView>(`/learn/concepts/${slug}/view`, { method: "POST" }).then(setProgress).catch(() => {});
  }, [slug]);

  async function update(body: { completed?: boolean; bookmarked?: boolean }, which: "complete" | "bookmark") {
    setBusy(which);
    try {
      setProgress(await api<ProgressView>(`/learn/concepts/${slug}/progress`, { method: "PUT", body: JSON.stringify(body) }));
      router.refresh();
    } finally {
      setBusy(null);
    }
  }

  const completed = progress.status === "COMPLETED";

  return (
    <div className="flex flex-wrap items-center gap-2">
      <button
        type="button"
        onClick={() => update({ completed: !completed }, "complete")}
        disabled={busy !== null}
        className={clsx(
          "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition disabled:opacity-60",
          completed
            ? "border-success/40 bg-success/10 text-success"
            : "border-border text-fg-muted hover:bg-bg-hover hover:text-fg",
        )}
      >
        {busy === "complete" ? (
          <Loader2 className="size-4 animate-spin" />
        ) : completed ? (
          <CheckCircle2 className="size-4" />
        ) : (
          <Circle className="size-4" />
        )}
        {completed ? "Completed" : "Mark complete"}
      </button>
      <button
        type="button"
        onClick={() => update({ bookmarked: !progress.bookmarked }, "bookmark")}
        disabled={busy !== null}
        aria-pressed={progress.bookmarked}
        className={clsx(
          "inline-flex items-center gap-1.5 rounded-md border px-3 py-1.5 text-sm transition disabled:opacity-60",
          progress.bookmarked
            ? "border-warning/40 bg-warning/10 text-warning"
            : "border-border text-fg-muted hover:bg-bg-hover hover:text-fg",
        )}
      >
        {busy === "bookmark" ? (
          <Loader2 className="size-4 animate-spin" />
        ) : (
          <Bookmark className={clsx("size-4", progress.bookmarked && "fill-current")} />
        )}
        {progress.bookmarked ? "Bookmarked" : "Bookmark for revision"}
      </button>
    </div>
  );
}
