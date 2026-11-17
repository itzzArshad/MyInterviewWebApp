"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import clsx from "clsx";
import { Check, CheckCircle2, ChevronRight, Eye, Loader2, Mic, Square, XCircle } from "lucide-react";
import type { InterviewQuestion, PracticePayload, PracticeResult } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";
import { useRecorder } from "./useRecorder";
import { Timer } from "./Timer";

/** A structure to speak in — the habit that makes answers sound senior. */
function scaffoldFor(q: InterviewQuestion) {
  return q.subjectSlug === "system-design"
    ? ["Clarify the problem", "Requirements & scale", "High-level design", "Deep dive", "Trade-offs"]
    : ["What it is", "Why it exists", "How it works inside", "Concrete example", "Trade-offs & pitfalls"];
}

function codeBlock(code: string | null) {
  return code ? `\n\n\`\`\`java\n${code}\n\`\`\`` : "";
}

interface Props {
  question: InterviewQuestion;
  submit: (payload: Omit<PracticePayload, "key">) => Promise<PracticeResult>;
  onNext?: () => void;
  nextLabel?: string;
}

/**
 * Runs one interview question:
 *  open questions → answer out loud (timer + optional recording) → reveal model answer → tick the key points
 *                   you covered → follow-ups → score;
 *  option questions → choose → checked by the server.
 */
export function QuestionRunner({ question, submit, onNext, nextLabel = "Next question" }: Props) {
  const [phase, setPhase] = useState<"answer" | "review" | "done">("answer");
  const [elapsed, setElapsed] = useState(0);
  const [covered, setCovered] = useState<Set<number>>(new Set());
  const [followRevealed, setFollowRevealed] = useState(false);
  const [followUpOk, setFollowUpOk] = useState<boolean | null>(null);
  const [chosen, setChosen] = useState<string | null>(null);
  const [note, setNote] = useState("");
  const [result, setResult] = useState<PracticeResult | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const started = useRef(Date.now());
  const rec = useRecorder();

  useEffect(() => {
    if (phase !== "answer") return;
    const t = setInterval(() => setElapsed(Math.floor((Date.now() - started.current) / 1000)), 500);
    return () => clearInterval(t);
  }, [phase]);

  const stopAnswering = () => {
    if (rec.recording) rec.stop();
    setPhase("review");
  };

  async function send(payload: Omit<PracticePayload, "key">) {
    setBusy(true);
    setError(null);
    try {
      setResult(await submit(payload));
      setPhase("done");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }

  const toggle = (i: number) =>
    setCovered((s) => {
      const n = new Set(s);
      if (n.has(i)) n.delete(i);
      else n.add(i);
      return n;
    });

  return (
    <div className="space-y-5">
      {/* Question */}
      <div className="rounded-xl border border-border p-5">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-3 text-xs text-fg-faint">
          <Link href={`/learn/${question.conceptSlug}`} className="hover:text-fg">
            {question.subjectTitle} · {question.conceptTitle}
          </Link>
          {phase === "answer" && <Timer elapsed={elapsed} limit={question.limitSec} />}
        </div>
        <InlineMarkdown className="text-lg font-medium">{question.question + codeBlock(question.code)}</InlineMarkdown>
      </div>

      {/* ---------- option (auto-graded) questions ---------- */}
      {question.autoGraded && question.options && (
        <div className="space-y-2">
          {question.options.map((o, i) => {
            const value = question.options!.length === 2 && o === "True" ? "true" : question.options!.length === 2 && o === "False" ? "false" : String(i);
            const isPicked = chosen === value;
            const correctLabel = result?.correctAnswer ?? "";
            const isCorrect = phase === "done" && (correctLabel === o || correctLabel.startsWith(`${String.fromCharCode(65 + i)}.`));
            return (
              <button
                key={i}
                disabled={phase === "done" || busy}
                onClick={() => setChosen(value)}
                className={clsx(
                  "flex w-full items-start gap-3 rounded-lg border px-3.5 py-2.5 text-left text-sm transition",
                  isCorrect && "border-success bg-success/10",
                  phase === "done" && isPicked && !isCorrect && "border-danger bg-danger/10",
                  phase !== "done" && isPicked && "border-accent bg-accent-soft",
                  phase !== "done" && !isPicked && "border-border hover:bg-bg-subtle",
                )}
              >
                <span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full border border-border text-[11px]">
                  {String.fromCharCode(65 + i)}
                </span>
                <InlineMarkdown className="min-w-0 flex-1">{o}</InlineMarkdown>
              </button>
            );
          })}
          {phase !== "done" && (
            <button
              onClick={() => send({ durationSec: elapsed, chosen: chosen ?? undefined })}
              disabled={chosen === null || busy}
              className="mt-2 inline-flex items-center gap-2 rounded-md bg-fg px-4 py-2 text-sm font-medium text-bg disabled:opacity-40"
            >
              {busy && <Loader2 className="size-4 animate-spin" />} Check answer
            </button>
          )}
        </div>
      )}

      {/* ---------- open questions: answer out loud ---------- */}
      {!question.autoGraded && phase === "answer" && (
        <div className="rounded-xl border border-border bg-bg-subtle p-5">
          <p className="text-sm font-medium">Answer out loud, as if the interviewer is listening.</p>
          <ol className="mt-3 flex flex-wrap gap-2 text-xs">
            {scaffoldFor(question).map((s, i) => (
              <li key={s} className="rounded-full border border-border bg-bg px-2.5 py-1 text-fg-muted">
                {i + 1}. {s}
              </li>
            ))}
          </ol>
          <div className="mt-4 flex flex-wrap items-center gap-2">
            {rec.supported &&
              (rec.recording ? (
                <button onClick={rec.stop} className="inline-flex items-center gap-1.5 rounded-md border border-danger/40 bg-danger/10 px-3 py-1.5 text-sm text-danger">
                  <Square className="size-3.5 fill-current" /> Stop recording
                </button>
              ) : (
                <button onClick={rec.start} className="inline-flex items-center gap-1.5 rounded-md border border-border bg-bg px-3 py-1.5 text-sm hover:bg-bg-hover">
                  <Mic className="size-4" /> {rec.url ? "Record again" : "Record my answer"}
                </button>
              ))}
            <button onClick={stopAnswering} className="inline-flex items-center gap-1.5 rounded-md bg-accent px-4 py-1.5 text-sm font-medium text-white hover:opacity-90">
              <Eye className="size-4" /> I&apos;m done — show the model answer
            </button>
          </div>
          {rec.recording && <p className="mt-2 flex items-center gap-1.5 text-xs text-danger"><span className="size-2 animate-pulse rounded-full bg-danger" /> Recording…</p>}
          {rec.error && <p className="mt-2 text-xs text-warning">{rec.error}</p>}
        </div>
      )}

      {!question.autoGraded && phase !== "answer" && (
        <div className="space-y-5">
          {rec.url && (
            <div className="rounded-xl border border-border p-4">
              <p className="mb-2 text-sm font-medium">Your answer — listen back: was it structured, precise, without filler?</p>
              <audio controls src={rec.url} className="w-full" />
            </div>
          )}

          <div className="rounded-xl border border-border p-5">
            <p className="mb-2 text-xs font-semibold tracking-wide text-fg-faint uppercase">Model answer</p>
            <InlineMarkdown>{question.modelAnswer}</InlineMarkdown>
          </div>

          <div className="rounded-xl border border-border p-5">
            <p className="text-sm font-medium">Which key points did you actually say?</p>
            <p className="mt-0.5 text-xs text-fg-faint">Be strict — this is what an interviewer listens for.</p>
            <ul className="mt-3 space-y-2">
              {question.keyPoints.map((k, i) => (
                <li key={i}>
                  <button
                    disabled={phase === "done"}
                    onClick={() => toggle(i)}
                    className={clsx(
                      "flex w-full items-start gap-2.5 rounded-lg border px-3 py-2 text-left text-sm transition",
                      covered.has(i) ? "border-success/50 bg-success/10" : "border-border hover:bg-bg-subtle",
                    )}
                  >
                    <span className={clsx("mt-0.5 flex size-4 shrink-0 items-center justify-center rounded border", covered.has(i) ? "border-success bg-success text-white" : "border-fg-faint")}>
                      {covered.has(i) && <Check className="size-3" />}
                    </span>
                    <InlineMarkdown className="min-w-0 flex-1">{k}</InlineMarkdown>
                  </button>
                </li>
              ))}
            </ul>
          </div>

          {question.followUps.length > 0 && (
            <div className="rounded-xl border border-border p-5">
              <p className="text-xs font-semibold tracking-wide text-fg-faint uppercase">Follow-up from the interviewer</p>
              <InlineMarkdown className="mt-2 font-medium">{question.followUps[0].question}</InlineMarkdown>
              {!followRevealed ? (
                <button onClick={() => setFollowRevealed(true)} className="mt-3 text-sm text-accent underline-offset-2 hover:underline">
                  Answer it out loud, then reveal
                </button>
              ) : (
                <>
                  <InlineMarkdown className="mt-3 rounded-lg bg-bg-subtle p-3 text-sm">{question.followUps[0].answer || "_(no model answer)_"}</InlineMarkdown>
                  <div className="mt-3 flex gap-2">
                    {[true, false].map((v) => (
                      <button
                        key={String(v)}
                        disabled={phase === "done"}
                        onClick={() => setFollowUpOk(v)}
                        className={clsx(
                          "rounded-md border px-3 py-1.5 text-sm",
                          followUpOk === v ? (v ? "border-success bg-success/10 text-success" : "border-danger bg-danger/10 text-danger") : "border-border hover:bg-bg-hover",
                        )}
                      >
                        {v ? "I handled it" : "I didn't know"}
                      </button>
                    ))}
                  </div>
                </>
              )}
            </div>
          )}

          {phase === "review" && (
            <div className="space-y-3">
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                rows={2}
                placeholder="Optional: what to improve next time (e.g. 'forgot to mention treeification')"
                className="w-full rounded-lg border border-border bg-bg p-3 text-base outline-none focus:border-accent sm:text-sm"
              />
              <button
                onClick={() => send({ durationSec: elapsed, covered: [...covered], followUpOk, note: note || undefined })}
                disabled={busy}
                className="inline-flex items-center gap-2 rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
              >
                {busy && <Loader2 className="size-4 animate-spin" />} Score my answer ({covered.size}/{question.keyPoints.length})
              </button>
            </div>
          )}
        </div>
      )}

      {/* ---------- result ---------- */}
      {phase === "done" && result && (
        <div className={clsx("rounded-xl border p-5", result.scorePct >= 70 ? "border-success/40 bg-success/5" : "border-warning/40 bg-warning/5")}>
          <p className="flex items-center gap-2 font-semibold">
            {result.scorePct >= 70 ? <CheckCircle2 className="size-5 text-success" /> : <XCircle className="size-5 text-warning" />}
            {question.autoGraded ? (result.scorePct === 100 ? "Correct" : "Not quite") : `${result.scorePct}% of key points covered`}
            <span className="text-sm font-normal text-fg-muted">· {elapsed}s</span>
          </p>
          {question.autoGraded && <InlineMarkdown className="mt-3 text-sm">{question.modelAnswer}</InlineMarkdown>}
          {result.nextReview && <p className="mt-2 text-sm text-fg-muted">Comes back for revision {result.nextReview}.</p>}
          {onNext && (
            <button onClick={onNext} className="mt-4 inline-flex items-center gap-1.5 rounded-md bg-fg px-4 py-2 text-sm font-medium text-bg hover:opacity-90">
              {nextLabel} <ChevronRight className="size-4" />
            </button>
          )}
        </div>
      )}
      {error && <p className="text-sm text-danger">{error}</p>}
    </div>
  );
}
