import Link from "next/link";
import type { Metadata } from "next";
import { Mic, Target } from "lucide-react";
import { serverApi } from "@/lib/server-api";
import type { BankItem, InterviewHub } from "@/lib/types";
import { ProgressBar, MasteryPill } from "@/components/learn/badges";
import { StartMockButton } from "@/components/interview/StartMockButton";

export const metadata: Metadata = { title: "Interview" };

export default async function InterviewPage() {
  const [hub, bank] = await Promise.all([serverApi<InterviewHub>("/interview/hub"), serverApi<BankItem[]>("/interview/bank")]);
  const bySubject = new Map<string, BankItem[]>();
  bank.forEach((b) => bySubject.set(b.subjectTitle, [...(bySubject.get(b.subjectTitle) ?? []), b]));

  return (
    <div className="mx-auto max-w-4xl px-5 py-8 md:px-12 md:py-12">
      <h1 className="text-2xl font-bold tracking-tight md:text-3xl">Interview training</h1>
      <p className="mt-1 text-fg-muted">Say it out loud until it sounds like you have done it in production.</p>

      {/* Actions */}
      <div className="mt-6 grid gap-3 sm:grid-cols-2">
        <Link href="/interview/explain" className="group rounded-xl border border-border p-5 transition hover:bg-bg-subtle">
          <Mic className="size-5 text-accent" />
          <p className="mt-3 font-semibold">Explain practice</p>
          <p className="mt-1 text-sm text-fg-muted">One question at a time: answer out loud, compare with the model answer, score the key points you covered.</p>
        </Link>
        <div className="rounded-xl border border-border p-5">
          <Target className="size-5 text-accent" />
          <p className="mt-3 font-semibold">Mock interview</p>
          <p className="mt-1 text-sm text-fg-muted">~6 timed questions across Java, Spring, backend, a debugging scenario and design — then a report card.</p>
          <StartMockButton disabled={hub.bankSize === 0} />
        </div>
      </div>

      {/* Readiness */}
      <section className="mt-10">
        <div className="flex items-baseline justify-between">
          <h2 className="text-sm font-semibold text-fg-muted">Readiness</h2>
          <span className="text-2xl font-bold tabular-nums">{hub.readiness.overallPct}%</span>
        </div>
        <p className="mt-1 text-xs text-fg-faint">Per area: 40% lessons completed · 30% recall from revision · 30% how well you explain (last 60 days).</p>
        <ul className="mt-4 space-y-5">
          {hub.readiness.areas.map((a) => (
            <li key={a.subjectSlug}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="font-medium">{a.subjectTitle}</span>
                <span className="font-semibold tabular-nums">{a.readinessPct}%</span>
              </div>
              <ProgressBar value={a.readinessPct} className="mt-1.5" />
              <div className="mt-1.5 flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-fg-faint">
                <span>Lessons {a.completed}/{a.total} ({a.coveragePct}%)</span>
                <span>Recall {a.retentionPct}%</span>
                <span>Delivery {a.deliveryPct}%</span>
                <span className="text-fg-muted">→ {a.advice}</span>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* Recent mocks */}
      {hub.recentMocks.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-3 text-sm font-semibold text-fg-muted">Recent mock interviews</h2>
          <ul className="divide-y divide-border/60 rounded-lg border border-border">
            {hub.recentMocks.map((m) => (
              <li key={m.id}>
                <Link href={`/interview/mock/${m.id}`} className="flex items-center justify-between px-3 py-2.5 text-sm hover:bg-bg-subtle">
                  <span>{new Date(m.finishedAt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" })}</span>
                  <span className="text-fg-muted">{m.questions} questions · <strong className="text-fg">{m.scorePct ?? 0}%</strong></span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {/* Bank */}
      <section className="mt-10">
        <h2 className="mb-1 text-sm font-semibold text-fg-muted">Question bank · {hub.bankSize}</h2>
        <p className="mb-4 text-xs text-fg-faint">Built automatically from every lesson&apos;s interview section. Add lessons to grow it.</p>
        {bank.length === 0 && <p className="text-sm text-fg-faint">Empty — write a lesson first.</p>}
        <div className="space-y-6">
          {[...bySubject.entries()].map(([subject, items]) => (
            <div key={subject}>
              <h3 className="mb-1.5 text-xs font-semibold tracking-wide text-fg-faint uppercase">{subject}</h3>
              <ul className="divide-y divide-border/60 rounded-lg border border-border">
                {items.map((b) => (
                  <li key={b.key}>
                    <Link href={`/interview/explain?key=${b.key}`} className="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-bg-subtle">
                      <span className="min-w-0 flex-1">
                        <span className="line-clamp-2">{b.question.replace(/[*`_]/g, "")}</span>
                        <span className="text-xs text-fg-faint">{b.conceptTitle}</span>
                      </span>
                      {b.due && <span className="rounded bg-warning/15 px-1.5 py-0.5 text-[10px] font-medium text-warning">Due</span>}
                      {b.bestScore != null ? <MasteryPill value={b.bestScore} /> : <span className="text-xs text-fg-faint">new</span>}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
