"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import clsx from "clsx";
import { Clock, Lightbulb, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { MockReport, MockView, PracticeResult } from "@/lib/types";
import { ProgressBar } from "@/components/learn/badges";
import { QuestionRunner } from "./QuestionRunner";
import { StartMockButton } from "./StartMockButton";
import { formatSeconds } from "./Timer";

/** Runs a mock interview question by question (resumable after a reload), then shows the report card. */
export function MockRunner({ id }: { id: number }) {
  const [mock, setMock] = useState<MockView | null>(null);
  const [index, setIndex] = useState(0);
  const [report, setReport] = useState<MockReport | null>(null);
  const [finishing, setFinishing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<MockView>(`/interview/mocks/${id}`)
      .then(async (m) => {
        setMock(m);
        if (m.finishedAt) setReport(await api<MockReport>(`/interview/mocks/${id}/report`));
        else setIndex(Math.max(0, m.questions.findIndex((q) => !m.answeredKeys.includes(q.key))));
      })
      .catch((e: Error) => setError(e.message));
  }, [id]);

  async function finish() {
    setFinishing(true);
    try {
      setReport(await api<MockReport>(`/interview/mocks/${id}/finish`, { method: "POST" }));
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setFinishing(false);
    }
  }

  if (error) return <p className="text-sm text-danger">{error}</p>;
  if (!mock) return <div className="flex justify-center py-16 text-fg-faint"><Loader2 className="size-5 animate-spin" /></div>;
  if (report) return <Report report={report} />;

  const q = mock.questions[index];
  const isLast = index === mock.questions.length - 1;
  return (
    <>
      <div className="mb-5 flex items-center justify-between gap-3">
        <h1 className="text-xl font-bold tracking-tight">Mock interview</h1>
        <div className="flex items-center gap-3 text-sm text-fg-muted">
          Question {index + 1} / {mock.questions.length}
          <button onClick={finish} disabled={finishing} className="rounded-md border border-border px-2.5 py-1 text-xs hover:bg-bg-hover">
            End interview
          </button>
        </div>
      </div>
      <ProgressBar value={(index / mock.questions.length) * 100} className="mb-6" />
      <QuestionRunner
        key={q.key}
        question={q}
        submit={(p) => api<PracticeResult>(`/interview/mocks/${id}/answers`, { method: "POST", body: JSON.stringify({ key: q.key, ...p }) })}
        onNext={isLast ? finish : () => setIndex((i) => i + 1)}
        nextLabel={isLast ? "Finish and see report" : "Next question"}
      />
    </>
  );
}

function Report({ report }: { report: MockReport }) {
  const score = report.scorePct ?? 0;
  return (
    <div>
      <p className="text-sm text-fg-faint">
        <Link href="/interview" className="hover:text-fg">Interview</Link> / Report
      </p>
      <h1 className="mt-2 text-2xl font-bold tracking-tight">Mock interview report</h1>
      <div className="mt-6 flex items-center gap-5 rounded-xl border border-border p-5">
        <div className={clsx("text-4xl font-bold tabular-nums", score >= 80 ? "text-success" : score >= 60 ? "text-warning" : "text-danger")}>{score}%</div>
        <div className="text-sm text-fg-muted">
          Average share of key points covered (auto-graded questions count 0 or 100).
          {report.finishedAt && <> Finished {new Date(report.finishedAt).toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" })}.</>}
        </div>
      </div>

      <section className="mt-8">
        <h2 className="mb-3 text-sm font-semibold text-fg-muted">By area</h2>
        <ul className="space-y-3">
          {report.areas.map((a) => (
            <li key={a.subjectSlug}>
              <div className="flex justify-between text-sm"><span>{a.subjectTitle} <span className="text-fg-faint">· {a.questions}</span></span><span className="font-medium">{a.scorePct}%</span></div>
              <ProgressBar value={a.scorePct} className="mt-1" />
            </li>
          ))}
        </ul>
      </section>

      <section className="mt-8 space-y-2">
        {report.advice.map((a, i) => (
          <p key={i} className="flex gap-2 rounded-lg bg-bg-subtle p-3 text-sm"><Lightbulb className="mt-0.5 size-4 shrink-0 text-warning" />{a}</p>
        ))}
      </section>

      <section className="mt-8">
        <h2 className="mb-3 text-sm font-semibold text-fg-muted">Questions</h2>
        <ul className="divide-y divide-border/60 rounded-lg border border-border">
          {report.items.map((it) => (
            <li key={it.key}>
              <Link href={`/interview/explain?key=${it.key}`} className="flex items-center gap-3 px-3 py-2.5 text-sm hover:bg-bg-subtle">
                <span className="min-w-0 flex-1">
                  <span className="line-clamp-2">{it.question.replace(/[*`_]/g, "")}</span>
                  <span className="flex items-center gap-1 text-xs text-fg-faint">
                    {it.subjectTitle}
                    {it.answered && (
                      <span className={clsx("ml-2 inline-flex items-center gap-0.5", it.durationSec > it.limitSec && "text-danger")}>
                        <Clock className="size-3" />{formatSeconds(it.durationSec)}
                      </span>
                    )}
                    {it.followUpOk === false && <span className="ml-2 text-danger">follow-up missed</span>}
                  </span>
                </span>
                <span className={clsx("text-sm font-semibold tabular-nums", !it.answered ? "text-fg-faint" : (it.scorePct ?? 0) >= 70 ? "text-success" : "text-danger")}>
                  {it.answered ? `${it.scorePct}%` : "skipped"}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      </section>
      <div className="mt-8"><StartMockButton /></div>
    </div>
  );
}
