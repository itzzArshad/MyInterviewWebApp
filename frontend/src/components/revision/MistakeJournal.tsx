"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import clsx from "clsx";
import { CheckCircle2, Circle, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { MistakeView } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";

/** Every question you got wrong, your answer vs the right one, and your own note on why. */
export function MistakeJournal() {
  const [items, setItems] = useState<MistakeView[] | null>(null);
  const [open, setOpen] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<MistakeView[]>("/revision/mistakes").then(setItems).catch((e: Error) => setError(e.message));
  }, []);

  async function update(m: MistakeView, patch: { note?: string; resolved?: boolean }) {
    setItems((list) => list?.map((x) => (x.id === m.id ? { ...x, ...patch } : x)) ?? null);
    try {
      await api(`/revision/mistakes/${m.id}`, { method: "PATCH", body: JSON.stringify(patch) });
    } catch (e) {
      setError((e as Error).message);
    }
  }

  if (error) return <p className="text-sm text-danger">{error}</p>;
  if (!items) return <div className="flex justify-center py-16 text-fg-faint"><Loader2 className="size-5 animate-spin" /></div>;
  if (items.length === 0) return <p className="text-sm text-fg-muted">No mistakes recorded yet. Wrong quiz answers land here automatically.</p>;

  const openCount = items.filter((m) => !m.resolved).length;
  return (
    <>
      <p className="mb-4 text-sm text-fg-muted">
        {openCount} open · write <em>why</em> you got each one wrong — that reflection is what changes how you think.
      </p>
      <ul className="space-y-2">
        {items.map((m) => (
          <li key={m.id} className={clsx("rounded-xl border border-border", m.resolved && "opacity-60")}>
            <button onClick={() => setOpen(open === m.id ? null : m.id)} className="flex w-full items-start gap-3 p-4 text-left">
              {m.resolved ? <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-success" /> : <Circle className="mt-0.5 size-4 shrink-0 text-danger" />}
              <span className="min-w-0 flex-1">
                <span className="line-clamp-2 text-sm font-medium">{m.prompt.replace(/[*`_]/g, "")}</span>
                <span className="text-xs text-fg-faint">
                  {m.conceptTitle} · wrong {m.times}× · last {new Date(m.lastWrongAt).toLocaleDateString()}
                </span>
              </span>
            </button>
            {open === m.id && (
              <div className="space-y-4 border-t border-border p-4">
                <InlineMarkdown>{m.prompt + (m.code ? `\n\n\`\`\`java\n${m.code}\n\`\`\`` : "")}</InlineMarkdown>
                <div className="grid gap-2 text-sm sm:grid-cols-2">
                  <div className="rounded-lg bg-danger/10 p-3"><p className="text-xs text-danger">You answered</p><p className="mt-1 whitespace-pre-wrap">{m.yourAnswer ?? "—"}</p></div>
                  <div className="rounded-lg bg-success/10 p-3"><p className="text-xs text-success">Correct</p><p className="mt-1 whitespace-pre-wrap">{m.correctAnswer ?? "—"}</p></div>
                </div>
                <InlineMarkdown className="rounded-lg bg-bg-subtle p-3 text-sm">{m.explanation}</InlineMarkdown>
                <label className="block">
                  <span className="text-xs font-medium text-fg-muted">Why did I get this wrong?</span>
                  <textarea
                    defaultValue={m.note ?? ""}
                    onBlur={(e) => e.target.value !== (m.note ?? "") && update(m, { note: e.target.value })}
                    rows={2}
                    placeholder="e.g. I assumed == compares content; it compares references."
                    className="mt-1 w-full rounded-lg border border-border bg-bg p-3 text-base outline-none focus:border-accent sm:text-sm"
                  />
                </label>
                <div className="flex flex-wrap items-center gap-3">
                  <button onClick={() => update(m, { resolved: !m.resolved })} className="rounded-md border border-border px-3 py-1.5 text-sm hover:bg-bg-hover">
                    {m.resolved ? "Reopen" : "I understand it now"}
                  </button>
                  <Link href={`/learn/${m.conceptSlug}`} className="text-sm text-accent underline-offset-2 hover:underline">Open the lesson</Link>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </>
  );
}
