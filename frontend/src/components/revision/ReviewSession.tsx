"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import clsx from "clsx";
import { Bookmark, CheckCircle2, Eye, Loader2, PartyPopper } from "lucide-react";
import { api } from "@/lib/api";
import type { GradeResult, ReviewCardView, ReviewQueue, RevisionStats } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";
import { ProgressBar } from "@/components/learn/badges";

const GRADES = [
  { value: 1, label: "Again", cls: "border-danger/40 text-danger hover:bg-danger/10" },
  { value: 2, label: "Hard", cls: "border-warning/40 text-warning hover:bg-warning/10" },
  { value: 3, label: "Good", cls: "border-success/40 text-success hover:bg-success/10" },
  { value: 4, label: "Easy", cls: "border-accent/40 text-accent hover:bg-accent-soft" },
];

const TYPE_LABEL = { QUIZ: "Recall", EXPLAIN: "Explain out loud", INTERVIEW: "Interview question" } as const;

/** Anki-style review: front → think/say the answer → reveal → rate honestly (1–4). Keyboard: Space, 1–4. */
export function ReviewSession() {
  const [cards, setCards] = useState<ReviewCardView[] | null>(null);
  const [stats, setStats] = useState<RevisionStats | null>(null);
  const [i, setI] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [picked, setPicked] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [doneNow, setDoneNow] = useState(0);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      const q = await api<ReviewQueue>("/revision/queue?limit=20");
      setCards(q.cards);
      setStats(q.stats);
      setI(0);
      setRevealed(false);
      setPicked(null);
    } catch (e) {
      setError((e as Error).message);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const card = cards?.[i];

  const grade = useCallback(
    async (g: number) => {
      if (!card || busy) return;
      setBusy(true);
      try {
        const r = await api<GradeResult>(`/revision/cards/${card.id}/grade`, { method: "POST", body: JSON.stringify({ grade: g }) });
        setStats(r.stats);
        setDoneNow((n) => n + 1);
        setI((x) => x + 1);
        setRevealed(false);
        setPicked(null);
      } catch (e) {
        setError((e as Error).message);
      } finally {
        setBusy(false);
      }
    },
    [card, busy],
  );

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (!card || (e.target as HTMLElement)?.tagName === "TEXTAREA") return;
      if (!revealed && (e.key === " " || e.key === "Enter")) {
        e.preventDefault();
        setRevealed(true);
      } else if (revealed && ["1", "2", "3", "4"].includes(e.key)) {
        grade(Number(e.key));
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [card, revealed, grade]);

  if (error) return <p className="text-sm text-danger">{error}</p>;
  if (!cards || !stats) return <div className="flex justify-center py-16 text-fg-faint"><Loader2 className="size-5 animate-spin" /></div>;

  const goalPct = Math.min(100, (stats.reviewedToday / stats.dailyGoal) * 100);
  const header = (
    <div className="mb-5">
      <div className="flex flex-wrap items-baseline justify-between gap-2 text-sm">
        <span className="text-fg-muted">
          Today: <strong className="text-fg">{stats.reviewedToday}</strong> / {stats.dailyGoal} reviewed · {stats.dueNow} due
        </span>
        <span className="text-xs text-fg-faint">
          {stats.totalCards} cards{stats.retentionPct != null && ` · est. recall ${stats.retentionPct}%`}
        </span>
      </div>
      <ProgressBar value={goalPct} className="mt-2" />
    </div>
  );

  if (!card) {
    return (
      <>
        {header}
        <div className="rounded-xl border border-border p-8 text-center">
          {stats.totalCards === 0 ? (
            <>
              <p className="font-medium">No cards yet</p>
              <p className="mt-1 text-sm text-fg-muted">Take a lesson quiz — every question becomes a revision card, and wrong answers come back tomorrow.</p>
              <Link href="/learn" className="mt-4 inline-block text-sm text-accent underline">Go to lessons</Link>
            </>
          ) : stats.dueNow > 0 ? (
            <>
              <CheckCircle2 className="mx-auto size-8 text-success" />
              <p className="mt-2 font-medium">{doneNow} reviewed — {stats.dueNow} more due</p>
              <button onClick={load} className="mt-4 rounded-md bg-accent px-4 py-2 text-sm font-medium text-white">Continue</button>
            </>
          ) : (
            <>
              <PartyPopper className="mx-auto size-8 text-success" />
              <p className="mt-2 font-medium">All caught up{doneNow > 0 ? ` — ${doneNow} reviewed` : ""}</p>
              <p className="mt-1 text-sm text-fg-muted">Come back tomorrow. Cards reappear right before you would forget them.</p>
              <Link href="/today" className="mt-4 inline-block text-sm text-accent underline">Back to today&apos;s session</Link>
            </>
          )}
        </div>
      </>
    );
  }

  const options = card.options;
  return (
    <>
      {header}
      <div className="rounded-xl border border-border p-5 md:p-6">
        <div className="mb-3 flex flex-wrap items-center gap-2 text-xs text-fg-faint">
          <span className="rounded bg-bg-hover px-1.5 py-0.5 font-medium text-fg-muted">{TYPE_LABEL[card.type]}</span>
          <Link href={`/learn/${card.conceptSlug}`} className="hover:text-fg">{card.conceptTitle}</Link>
          {card.bookmarked && <Bookmark className="size-3.5 fill-current text-warning" />}
          {card.overdueDays > 0 && <span className="text-warning">overdue {card.overdueDays}d</span>}
          <span className="ml-auto">{i + 1} / {cards.length}</span>
        </div>

        <InlineMarkdown className="font-medium">{card.prompt + (card.code ? `\n\n\`\`\`java\n${card.code}\n\`\`\`` : "")}</InlineMarkdown>

        {options && (
          <div className="mt-4 grid gap-2">
            {options.map((o, idx) => (
              <button
                key={idx}
                disabled={revealed}
                onClick={() => setPicked(idx)}
                className={clsx(
                  "flex items-start gap-3 rounded-lg border px-3.5 py-2 text-left text-sm",
                  revealed && idx === card.correctOption && "border-success bg-success/10",
                  revealed && picked === idx && idx !== card.correctOption && "border-danger bg-danger/10",
                  !revealed && picked === idx && "border-accent bg-accent-soft",
                  !revealed && picked !== idx && "border-border hover:bg-bg-subtle",
                  revealed && idx !== card.correctOption && picked !== idx && "border-border opacity-60",
                )}
              >
                <span className="mt-0.5 text-xs text-fg-faint">{String.fromCharCode(65 + idx)}</span>
                <InlineMarkdown className="min-w-0 flex-1">{o}</InlineMarkdown>
              </button>
            ))}
          </div>
        )}

        {!revealed ? (
          <button onClick={() => setRevealed(true)} className="mt-5 inline-flex items-center gap-2 rounded-md bg-fg px-4 py-2 text-sm font-medium text-bg">
            <Eye className="size-4" /> Show answer <kbd className="hidden text-[10px] opacity-60 md:inline">Space</kbd>
          </button>
        ) : (
          <div className="mt-5 space-y-4 border-t border-border pt-5">
            {card.answer && <p className="text-sm"><span className="text-fg-faint">Answer: </span><strong>{card.answer}</strong></p>}
            {card.keyPoints.length > 0 && (
              <div>
                <p className="mb-1.5 text-xs font-semibold tracking-wide text-fg-faint uppercase">Did you cover</p>
                <ul className="list-disc space-y-1 pl-5 text-sm">
                  {card.keyPoints.map((k, idx) => <li key={idx}><InlineMarkdown>{k}</InlineMarkdown></li>)}
                </ul>
              </div>
            )}
            {card.explanation && (
              <details open={card.type !== "EXPLAIN"} className="rounded-lg bg-bg-subtle p-3 text-sm">
                <summary className="cursor-pointer text-xs font-semibold tracking-wide text-fg-faint uppercase">
                  {card.type === "EXPLAIN" ? "Revision notes" : "Explanation"}
                </summary>
                <InlineMarkdown className="mt-2">{card.explanation}</InlineMarkdown>
              </details>
            )}
            <div>
              <p className="mb-2 text-xs text-fg-faint">How well did you remember it?</p>
              <div className="grid grid-cols-4 gap-2">
                {GRADES.map((g) => (
                  <button
                    key={g.value}
                    onClick={() => grade(g.value)}
                    disabled={busy}
                    className={clsx(
                      "rounded-lg border px-2 py-2 text-sm font-medium transition disabled:opacity-50",
                      g.cls,
                      picked !== null && card.correctOption !== null && picked !== card.correctOption && g.value === 1 && "ring-2 ring-danger/40",
                    )}
                  >
                    {g.label}
                    <span className="block text-[11px] font-normal opacity-70">{card.nextIntervals[String(g.value)]}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </>
  );
}
