"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { CheckCircle2, Eye, Lightbulb, Loader2, RotateCcw, Trophy, XCircle } from "lucide-react";
import { api } from "@/lib/api";
import type { AnswerResult, AttemptView, FinishResult, QuestionType, QuestionView } from "@/lib/types";
import { InlineMarkdown } from "@/components/markdown/InlineMarkdown";
import { DifficultyBadge, ProgressBar } from "./badges";

const TYPE_LABEL: Record<QuestionType, string> = {
  MCQ: "Multiple choice",
  TRUE_FALSE: "True or false",
  PREDICT_OUTPUT: "Predict the output",
  FIND_BUG: "Find the bug",
  CODE: "Write the code",
  SCENARIO: "Scenario",
  EXPLAIN: "Explain in your own words",
  INTERVIEW: "Interview question",
};

interface Props {
  conceptSlug: string;
  questionCount: number;
  lastQuiz: { score: number; total: number } | null;
  /** Server-highlighted code snippets keyed by question id. */
  codeHtml: Record<number, string>;
}

type Phase =
  | { kind: "idle" }
  | { kind: "running"; attempt: AttemptView; index: number; results: Record<number, AnswerResult> }
  | { kind: "done"; attempt: AttemptView; results: Record<number, AnswerResult>; finish: FinishResult };

export function Quiz({ conceptSlug, questionCount, lastQuiz, codeHtml }: Props) {
  const router = useRouter();
  const [phase, setPhase] = useState<Phase>({ kind: "idle" });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function start(questionIds?: number[]) {
    setError(null);
    setLoading(true);
    try {
      const attempt = await api<AttemptView>("/quiz/attempts", {
        method: "POST",
        body: JSON.stringify({ conceptSlug, questionIds }),
      });
      setPhase({ kind: "running", attempt, index: 0, results: {} });
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function finish(attempt: AttemptView, results: Record<number, AnswerResult>) {
    setLoading(true);
    try {
      const f = await api<FinishResult>(`/quiz/attempts/${attempt.attemptId}/finish`, { method: "POST" });
      setPhase({ kind: "done", attempt, results, finish: f });
      router.refresh(); // update mastery / dashboard data rendered on the server
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  if (phase.kind === "idle") {
    return (
      <div className="rounded-xl border border-border p-6">
        <p className="font-medium">Check your understanding</p>
        <p className="mt-1 text-sm text-fg-muted">
          {questionCount} questions, easy → hard. Hints reduce the credit a little; explanations appear after each answer.
          {lastQuiz && (
            <> Last attempt: <strong className="text-fg">{lastQuiz.score}/{lastQuiz.total}</strong>.</>
          )}
        </p>
        <button
          onClick={() => start()}
          disabled={loading}
          className="mt-4 inline-flex items-center gap-2 rounded-md bg-accent px-4 py-2 text-sm font-medium text-white transition hover:opacity-90 disabled:opacity-60"
        >
          {loading && <Loader2 className="size-4 animate-spin" />}
          {lastQuiz ? "Retake quiz" : "Start quiz"}
        </button>
        {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      </div>
    );
  }

  if (phase.kind === "done") {
    const { finish: f, attempt } = phase;
    const wrong = attempt.questions.filter((q) => f.incorrectQuestionIds.includes(q.id));
    return (
      <div className="rounded-xl border border-border p-6">
        <div className="flex items-center gap-3">
          <Trophy className={clsx("size-8", f.pct >= 80 ? "text-success" : f.pct >= 50 ? "text-warning" : "text-danger")} />
          <div>
            <p className="text-2xl font-bold tabular-nums">
              {f.score}/{f.total} <span className="text-base font-medium text-fg-muted">({f.pct}%)</span>
            </p>
            <p className="text-sm text-fg-muted">Concept mastery is now <strong className="text-fg">{f.mastery}%</strong></p>
          </div>
        </div>
        {wrong.length > 0 && (
          <div className="mt-5">
            <p className="text-sm font-medium">Review these:</p>
            <ul className="mt-2 space-y-1.5 text-sm text-fg-muted">
              {wrong.map((q) => (
                <li key={q.id} className="flex gap-2">
                  <XCircle className="mt-0.5 size-4 shrink-0 text-danger" />
                  <span className="line-clamp-2">{q.prompt.replace(/[*`_]/g, "")}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="mt-6 flex flex-wrap gap-2">
          {wrong.length > 0 && (
            <button
              onClick={() => start(wrong.map((q) => q.id))}
              disabled={loading}
              className="inline-flex items-center gap-2 rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-60"
            >
              <RotateCcw className="size-4" /> Retry {wrong.length} incorrect
            </button>
          )}
          <button
            onClick={() => start()}
            disabled={loading}
            className="rounded-md border border-border px-4 py-2 text-sm text-fg-muted hover:bg-bg-hover hover:text-fg disabled:opacity-60"
          >
            Retake all
          </button>
        </div>
        {error && <p className="mt-3 text-sm text-danger">{error}</p>}
      </div>
    );
  }

  const { attempt, index, results } = phase;
  const q = attempt.questions[index];
  const total = attempt.questions.length;
  const isLast = index === total - 1;

  return (
    <div className="rounded-xl border border-border p-5 md:p-6">
      <div className="mb-4 flex items-center justify-between gap-3 text-xs text-fg-muted">
        <span>
          Question {index + 1} of {total} · {TYPE_LABEL[q.type]}
        </span>
        <DifficultyBadge value={q.difficulty} />
      </div>
      <ProgressBar value={(index / total) * 100} className="mb-5" />

      <QuestionCard
        key={`${attempt.attemptId}-${q.id}`}
        q={q}
        codeHtml={codeHtml[q.id]}
        attemptId={attempt.attemptId}
        result={results[q.id]}
        onAnswered={(r) => setPhase({ ...phase, results: { ...results, [q.id]: r } })}
      />

      {results[q.id] && (
        <div className="mt-5 flex justify-end">
          <button
            onClick={() => (isLast ? finish(attempt, results) : setPhase({ ...phase, index: index + 1 }))}
            disabled={loading}
            className="inline-flex items-center gap-2 rounded-md bg-accent px-4 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-60"
          >
            {loading && <Loader2 className="size-4 animate-spin" />}
            {isLast ? "See results" : "Next question"}
          </button>
        </div>
      )}
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
    </div>
  );
}

// ---------------------------------------------------------------- one question

export function QuestionCard({
  q,
  codeHtml,
  attemptId,
  result,
  onAnswered,
}: {
  q: QuestionView;
  codeHtml?: string;
  attemptId: number;
  result?: AnswerResult;
  onAnswered: (r: AnswerResult) => void;
}) {
  const [selected, setSelected] = useState<string | null>(null);
  const [text, setText] = useState("");
  const [hintsShown, setHintsShown] = useState(0);
  const [revealed, setRevealed] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const options =
    q.type === "TRUE_FALSE"
      ? [
          { value: "true", label: "True" },
          { value: "false", label: "False" },
        ]
      : q.options?.map((label, i) => ({ value: String(i), label }));
  const answered = !!result;
  const hints = q.hints ?? [];

  async function submit(answer: string | null, selfCorrect?: boolean) {
    setSubmitting(true);
    setError(null);
    try {
      const r = await api<AnswerResult>(`/quiz/attempts/${attemptId}/answers`, {
        method: "POST",
        body: JSON.stringify({ questionId: q.id, answer, hintsUsed: hintsShown, selfCorrect }),
      });
      onAnswered(r);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div>
      <InlineMarkdown className="font-medium">{q.prompt}</InlineMarkdown>

      {q.code &&
        (codeHtml ? (
          <div className="quiz-code mt-4" dangerouslySetInnerHTML={{ __html: codeHtml }} />
        ) : (
          <pre className="quiz-code mt-4 overflow-x-auto p-4 font-mono text-sm">{q.code}</pre>
        ))}

      {/* Options */}
      {options && (
        <div className="mt-4 grid gap-2">
          {options.map((o) => {
            const isCorrect = answered && result!.correctAnswer === o.value;
            const isWrongPick = answered && selected === o.value && !result!.correct;
            return (
              <button
                key={o.value}
                type="button"
                disabled={answered || submitting}
                onClick={() => setSelected(o.value)}
                className={clsx(
                  "flex items-start gap-3 rounded-lg border px-3.5 py-2.5 text-left text-sm transition",
                  isCorrect && "border-success bg-success/10",
                  isWrongPick && "border-danger bg-danger/10",
                  !answered && selected === o.value && "border-accent bg-accent-soft",
                  !answered && selected !== o.value && "border-border hover:bg-bg-subtle",
                  answered && !isCorrect && !isWrongPick && "border-border opacity-60",
                )}
              >
                <span
                  className={clsx(
                    "mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full border text-[11px] font-medium",
                    selected === o.value ? "border-accent text-accent" : "border-border text-fg-faint",
                  )}
                >
                  {q.type === "TRUE_FALSE" ? (o.value === "true" ? "T" : "F") : String.fromCharCode(65 + Number(o.value))}
                </span>
                <InlineMarkdown className="min-w-0 flex-1">{o.label}</InlineMarkdown>
              </button>
            );
          })}
        </div>
      )}

      {/* Free text (predict output / self-graded) */}
      {!options && (
        <textarea
          value={text}
          onChange={(e) => setText(e.target.value)}
          disabled={answered || (q.selfGraded && revealed)}
          rows={q.type === "PREDICT_OUTPUT" ? 4 : 6}
          placeholder={q.type === "PREDICT_OUTPUT" ? "Type the exact output…" : "Write your answer before revealing the model answer…"}
          className={clsx(
            "mt-4 w-full rounded-lg border border-border bg-bg p-3 text-base outline-none focus:border-accent sm:text-sm",
            q.type === "PREDICT_OUTPUT" || q.type === "CODE" ? "font-mono" : "",
          )}
        />
      )}

      {/* Hints */}
      {!answered && hints.length > 0 && (
        <div className="mt-4 space-y-2">
          {hints.slice(0, hintsShown).map((h, i) => (
            <div key={i} className="flex gap-2 rounded-lg bg-warning/10 px-3 py-2 text-sm">
              <Lightbulb className="mt-0.5 size-4 shrink-0 text-warning" />
              <InlineMarkdown>{h}</InlineMarkdown>
            </div>
          ))}
          {hintsShown < hints.length && (
            <button
              type="button"
              onClick={() => setHintsShown((n) => n + 1)}
              className="inline-flex items-center gap-1.5 text-sm text-fg-muted hover:text-fg"
            >
              <Lightbulb className="size-4" /> Show hint ({hints.length - hintsShown} left)
            </button>
          )}
        </div>
      )}

      {/* Self-graded: reveal model answer, then rate yourself */}
      {q.selfGraded && !answered && revealed && (
        <div className="mt-5 rounded-lg border border-border bg-bg-subtle p-4">
          <p className="mb-2 text-xs font-semibold tracking-wide text-fg-faint uppercase">Model answer</p>
          <InlineMarkdown>{q.modelAnswer ?? ""}</InlineMarkdown>
          <p className="mt-4 text-sm font-medium">Did your answer cover the key points?</p>
          <div className="mt-2 flex gap-2">
            <button
              onClick={() => submit(text || null, true)}
              disabled={submitting}
              className="inline-flex items-center gap-1.5 rounded-md border border-success/40 bg-success/10 px-3 py-1.5 text-sm text-success"
            >
              <CheckCircle2 className="size-4" /> Yes, I got it
            </button>
            <button
              onClick={() => submit(text || null, false)}
              disabled={submitting}
              className="inline-flex items-center gap-1.5 rounded-md border border-danger/40 bg-danger/10 px-3 py-1.5 text-sm text-danger"
            >
              <XCircle className="size-4" /> No, I missed it
            </button>
          </div>
        </div>
      )}

      {/* Submit */}
      {!answered && !(q.selfGraded && revealed) && (
        <div className="mt-5">
          {q.selfGraded ? (
            <button
              onClick={() => setRevealed(true)}
              className="inline-flex items-center gap-2 rounded-md border border-border px-4 py-2 text-sm hover:bg-bg-hover"
            >
              <Eye className="size-4" /> Reveal model answer
            </button>
          ) : (
            <button
              onClick={() => submit(options ? selected : text)}
              disabled={submitting || (options ? selected === null : !text.trim())}
              className="inline-flex items-center gap-2 rounded-md bg-fg px-4 py-2 text-sm font-medium text-bg transition hover:opacity-90 disabled:opacity-40"
            >
              {submitting && <Loader2 className="size-4 animate-spin" />}
              Check answer
            </button>
          )}
        </div>
      )}

      {/* Result + explanation */}
      {result && (
        <div
          className={clsx(
            "mt-5 rounded-lg border p-4",
            result.correct ? "border-success/40 bg-success/5" : "border-danger/40 bg-danger/5",
          )}
        >
          <p className={clsx("flex items-center gap-2 font-medium", result.correct ? "text-success" : "text-danger")}>
            {result.correct ? <CheckCircle2 className="size-5" /> : <XCircle className="size-5" />}
            {result.correct ? (hintsShown ? "Correct (with hints)" : "Correct") : "Not quite"}
          </p>
          {!result.correct && q.type === "PREDICT_OUTPUT" && !options && result.correctAnswer && (
            <pre className="quiz-code mt-3 overflow-x-auto p-3 font-mono text-sm">{result.correctAnswer}</pre>
          )}
          {!q.selfGraded && <InlineMarkdown className="mt-3">{result.explanation}</InlineMarkdown>}
        </div>
      )}
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
    </div>
  );
}
