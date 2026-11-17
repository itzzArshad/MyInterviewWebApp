"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { InterviewQuestion, PracticeResult } from "@/lib/types";
import { QuestionRunner } from "./QuestionRunner";

/** Explain mode: one question → answer out loud → score key points → next. */
export function ExplainPractice() {
  const router = useRouter();
  const params = useSearchParams();
  const key = params.get("key");
  const [question, setQuestion] = useState<InterviewQuestion | null>(null);
  const [empty, setEmpty] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [count, setCount] = useState(0);

  const next = useCallback(async () => {
    try {
      const r = await fetch("/api/interview/next-explain", { credentials: "same-origin" });
      if (r.status === 204) return setEmpty(true);
      if (!r.ok) throw new Error("Could not load the next question");
      const { key: k } = await r.json();
      router.replace(`/interview/explain?key=${k}`);
    } catch (e) {
      setError((e as Error).message);
    }
  }, [router]);

  useEffect(() => {
    setQuestion(null);
    if (!key) {
      next();
      return;
    }
    api<InterviewQuestion>(`/interview/questions/${key}`).then(setQuestion).catch((e: Error) => setError(e.message));
  }, [key, next]);

  if (empty) {
    return (
      <p className="text-fg-muted">
        The interview bank is empty. <Link href="/learn" className="text-accent underline">Add a lesson</Link> — its interview section becomes practice questions.
      </p>
    );
  }
  if (error) return <p className="text-sm text-danger">{error}</p>;

  return (
    <>
      <div className="mb-6 flex items-baseline justify-between">
        <h1 className="text-2xl font-bold tracking-tight">Explain practice</h1>
        <Link href="/interview" className="text-sm text-fg-faint hover:text-fg">
          {count > 0 ? `${count} done · ` : ""}Interview hub
        </Link>
      </div>
      {!question ? (
        <div className="flex justify-center py-16 text-fg-faint">
          <Loader2 className="size-5 animate-spin" />
        </div>
      ) : (
        <QuestionRunner
          key={question.key}
          question={question}
          submit={async (p) => {
            const r = await api<PracticeResult>("/interview/practice", { method: "POST", body: JSON.stringify({ key: question.key, ...p }) });
            setCount((c) => c + 1);
            return r;
          }}
          onNext={next}
        />
      )}
    </>
  );
}
