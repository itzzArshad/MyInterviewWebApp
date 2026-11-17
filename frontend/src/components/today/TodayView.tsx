"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import clsx from "clsx";
import { ArrowRight, BookOpen, Brain, CheckCircle2, Flame, Loader2, Mic, PartyPopper, RotateCcw } from "lucide-react";
import { api } from "@/lib/api";
import type { AnswerResult, AttemptView, TodayStep, TodayView as Today } from "@/lib/types";
import { QuestionCard } from "@/components/learn/Quiz";

const ICONS = { revise: RotateCcw, learn: BookOpen, think: Brain, explain: Mic } as const;

function greeting() {
  const h = new Date().getHours();
  return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
}

/** The daily training session: revise → learn → think → explain. */
export function TodayView() {
  const [data, setData] = useState<Today | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    api<Today>("/today").then(setData).catch((e: Error) => setError(e.message));
  }, []);

  useEffect(() => {
    load();
    window.addEventListener("focus", load); // refresh after returning from revision / interview
    return () => window.removeEventListener("focus", load);
  }, [load]);

  if (error) return <p className="text-sm text-danger">{error}</p>;
  if (!data) return <div className="flex justify-center py-24 text-fg-faint"><Loader2 className="size-5 animate-spin" /></div>;

  const doneCount = data.steps.filter((s) => s.done).length;
  return (
    <>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-sm text-fg-faint">
            {new Date(data.date + "T00:00").toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "long" })}
          </p>
          <h1 className="mt-1 text-2xl font-bold tracking-tight md:text-3xl">{greeting()} — today&apos;s training</h1>
          <p className="mt-1 text-fg-muted">20–30 minutes. Small and daily beats long and rare.</p>
        </div>
        <div className="flex items-center gap-4">
          <div className="text-center">
            <div className={clsx("flex items-center gap-1 text-2xl font-bold tabular-nums", data.streak > 0 ? "text-warning" : "text-fg-faint")}>
              <Flame className="size-6" />
              {data.streak}
            </div>
            <div className="text-[11px] text-fg-faint">day streak</div>
          </div>
          <Ring done={doneCount} total={4} />
        </div>
      </div>

      {data.completed ? (
        <div className="mt-6 flex items-center gap-3 rounded-xl border border-success/40 bg-success/5 p-4">
          <PartyPopper className="size-6 shrink-0 text-success" />
          <div className="text-sm">
            <p className="font-semibold">Session complete — streak secured.</p>
            <p className="text-fg-muted">Anything more today is a bonus. Remaining steps still help.</p>
          </div>
        </div>
      ) : (
        <p className="mt-6 text-xs text-fg-faint">The day counts toward your streak when you finish <strong>Revise</strong>, <strong>Explain</strong> and either <strong>Learn</strong> or <strong>Think</strong>.</p>
      )}

      <ol className="mt-6 space-y-3">
        {data.steps.map((s, i) => (
          <StepCard key={s.key} step={s} index={i} data={data} onChanged={load} />
        ))}
      </ol>
    </>
  );
}

function Ring({ done, total }: { done: number; total: number }) {
  const r = 22;
  const c = 2 * Math.PI * r;
  return (
    <div className="relative size-14">
      <svg viewBox="0 0 56 56" className="size-14 -rotate-90">
        <circle cx="28" cy="28" r={r} fill="none" strokeWidth="5" className="stroke-bg-hover" />
        <circle cx="28" cy="28" r={r} fill="none" strokeWidth="5" strokeLinecap="round" className="stroke-success transition-all"
          strokeDasharray={c} strokeDashoffset={c * (1 - done / total)} />
      </svg>
      <span className="absolute inset-0 flex items-center justify-center text-sm font-semibold tabular-nums">{done}/{total}</span>
    </div>
  );
}

function StepCard({ step, index, data, onChanged }: { step: TodayStep; index: number; data: Today; onChanged: () => void }) {
  const Icon = ICONS[step.key];
  const [drillOpen, setDrillOpen] = useState(false);

  let action: React.ReactNode = null;
  if (step.key === "revise") {
    action = data.revision.dueNow > 0 || !step.done ? <Cta href="/revision">{data.revision.dueNow > 0 ? `Review ${Math.min(data.revision.dueNow, data.revision.dailyGoal)} cards` : "Open revision"}</Cta> : null;
  } else if (step.key === "learn" && data.learn) {
    action = <Cta href={`/learn/${data.learn.slug}`}>{data.learn.started ? "Continue" : "Start"}: {data.learn.title}</Cta>;
  } else if (step.key === "learn" && !data.learn) {
    action = <Cta href="/learn">Add a lesson</Cta>;
  } else if (step.key === "explain" && data.explainKey) {
    action = <Cta href={`/interview/explain?key=${data.explainKey}`}>Explain it out loud</Cta>;
  } else if (step.key === "think" && data.drill && !step.done) {
    action = !drillOpen ? (
      <button onClick={() => setDrillOpen(true)} className="inline-flex items-center gap-1.5 rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-white hover:opacity-90">
        Start drill <ArrowRight className="size-4" />
      </button>
    ) : null;
  }

  return (
    <li className={clsx("rounded-xl border p-4 md:p-5", step.done ? "border-success/40 bg-success/5" : "border-border")}>
      <div className="flex items-start gap-3">
        <span className={clsx("flex size-9 shrink-0 items-center justify-center rounded-lg", step.done ? "bg-success/15 text-success" : "bg-accent-soft text-accent")}>
          {step.done ? <CheckCircle2 className="size-5" /> : <Icon className="size-5" />}
        </span>
        <div className="min-w-0 flex-1">
          <p className="font-semibold">
            <span className="text-fg-faint">{index + 1}.</span> {step.title}
          </p>
          <p className="text-sm text-fg-muted">{step.description}</p>
          <p className="mt-1 truncate text-xs text-fg-faint">{step.detail}</p>
          {action && <div className="mt-3">{action}</div>}
          {step.key === "think" && drillOpen && data.drill && <Drill drill={data.drill} onFinished={onChanged} />}
        </div>
      </div>
    </li>
  );
}

function Cta({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link href={href} className="inline-flex max-w-full items-center gap-1.5 rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-white hover:opacity-90">
      <span className="truncate">{children}</span> <ArrowRight className="size-4 shrink-0" />
    </Link>
  );
}

/** One-question drill as a DRILL quiz attempt (doesn't change lesson mastery; wrong answers go to revision). */
function Drill({ drill, onFinished }: { drill: NonNullable<Today["drill"]>; onFinished: () => void }) {
  const [attempt, setAttempt] = useState<AttemptView | null>(null);
  const [result, setResult] = useState<AnswerResult | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<AttemptView>("/quiz/attempts", {
      method: "POST",
      body: JSON.stringify({ conceptSlug: drill.conceptSlug, questionIds: [drill.questionId], mode: "DRILL" }),
    })
      .then(setAttempt)
      .catch((e: Error) => setError(e.message));
  }, [drill]);

  if (error) return <p className="mt-3 text-sm text-danger">{error}</p>;
  if (!attempt) return <Loader2 className="mt-3 size-4 animate-spin text-fg-faint" />;

  const q = attempt.questions[0];
  return (
    <div className="mt-4 rounded-lg border border-border bg-bg p-4">
      <p className="mb-3 text-xs text-fg-faint">From: {drill.conceptTitle}</p>
      <QuestionCard
        q={q}
        attemptId={attempt.attemptId}
        result={result}
        onAnswered={async (r) => {
          if (result) return;
          setResult(r);
          await api(`/quiz/attempts/${attempt.attemptId}/finish`, { method: "POST" });
        }}
      />
      {result && (
        <button onClick={onFinished} className="mt-4 rounded-md bg-fg px-4 py-1.5 text-sm font-medium text-bg hover:opacity-90">
          Done — back to today
        </button>
      )}
    </div>
  );
}
