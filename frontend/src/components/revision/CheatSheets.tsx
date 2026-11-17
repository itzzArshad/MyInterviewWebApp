"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import clsx from "clsx";
import { Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { SummaryView } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";

/** Each studied lesson's "Summary and revision notes", weakest recall first — for the night before an interview. */
export function CheatSheets() {
  const [items, setItems] = useState<SummaryView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<SummaryView[]>("/revision/summaries").then(setItems).catch((e: Error) => setError(e.message));
  }, []);

  if (error) return <p className="text-sm text-danger">{error}</p>;
  if (!items) return <div className="flex justify-center py-16 text-fg-faint"><Loader2 className="size-5 animate-spin" /></div>;
  if (items.length === 0) return <p className="text-sm text-fg-muted">Lessons you have started appear here as quick cheat sheets.</p>;

  return (
    <div className="space-y-3">
      {items.map((s) => (
        <details key={s.slug} className="group rounded-xl border border-border">
          <summary className="flex cursor-pointer list-none items-center gap-3 p-4">
            <span className="min-w-0 flex-1">
              <span className="block font-medium">{s.title}</span>
              <span className="block truncate text-xs text-fg-faint">{s.subjectTitle}{s.summary ? ` · ${s.summary}` : ""}</span>
            </span>
            {s.retentionPct != null ? (
              <span className={clsx("rounded px-1.5 py-0.5 text-[11px] font-medium", s.retentionPct >= 85 ? "bg-success/15 text-success" : s.retentionPct >= 70 ? "bg-warning/15 text-warning" : "bg-danger/15 text-danger")}>
                recall {s.retentionPct}%
              </span>
            ) : (
              <span className="text-[11px] text-fg-faint">not revised yet</span>
            )}
          </summary>
          <div className="border-t border-border p-4">
            {s.notesMd ? <InlineMarkdown>{s.notesMd}</InlineMarkdown> : <p className="text-sm text-fg-faint">This lesson has no summary section.</p>}
            <Link href={`/learn/${s.slug}`} className="mt-3 inline-block text-sm text-accent underline-offset-2 hover:underline">Open full lesson</Link>
          </div>
        </details>
      ))}
    </div>
  );
}
