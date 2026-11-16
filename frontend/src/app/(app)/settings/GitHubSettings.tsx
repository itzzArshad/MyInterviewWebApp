"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { CloudDownload, CloudUpload, GitBranch, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { BulkResult, GitHubStatus } from "@/lib/types";

export function GitHubSettings({ status }: { status: GitHubStatus }) {
  const router = useRouter();
  const [busy, setBusy] = useState<"push" | "pull" | null>(null);
  const [result, setResult] = useState<{ title: string; r: BulkResult } | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function run(kind: "push" | "pull") {
    if (kind === "pull" && !confirm("Restore lessons from GitHub?\n\nLessons in the app are replaced by the versions in the repo when they differ. Progress and quiz history are kept.")) return;
    setBusy(kind);
    setError(null);
    setResult(null);
    try {
      const r = await api<BulkResult>(kind === "push" ? "/github/push-all" : "/github/pull", { method: "POST" });
      setResult({ title: kind === "push" ? "Backup to GitHub" : "Restore from GitHub", r });
      router.refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(null);
    }
  }

  return (
    <section className="mt-8 rounded-xl border border-border p-5">
      <h2 className="flex items-center gap-2 font-semibold">
        <GitBranch className="size-4" /> Lessons on GitHub
      </h2>
      {status.enabled ? (
        <>
          <p className="mt-1 text-sm text-fg-muted">
            Every lesson you save is committed to{" "}
            <a href={status.url ?? "#"} target="_blank" rel="noopener noreferrer" className="text-accent underline">
              {status.repo}
            </a>{" "}
            ({status.branch}). The database is what the app reads; the repo is your versioned backup.
          </p>
          <div className="mt-4 flex flex-wrap gap-2">
            <button
              onClick={() => run("push")}
              disabled={busy !== null}
              className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-sm hover:bg-bg-hover disabled:opacity-60"
            >
              {busy === "push" ? <Loader2 className="size-4 animate-spin" /> : <CloudUpload className="size-4" />} Back up all lessons
            </button>
            <button
              onClick={() => run("pull")}
              disabled={busy !== null}
              className="inline-flex items-center gap-1.5 rounded-md border border-border px-3 py-1.5 text-sm hover:bg-bg-hover disabled:opacity-60"
            >
              {busy === "pull" ? <Loader2 className="size-4 animate-spin" /> : <CloudDownload className="size-4" />} Restore from GitHub
            </button>
          </div>
        </>
      ) : (
        <p className="mt-1 text-sm text-fg-muted">
          Not configured. Set <code className="text-xs">GITHUB_CONTENT_TOKEN</code> and <code className="text-xs">GITHUB_CONTENT_REPO</code> on
          the backend (see docs/DEPLOYMENT.md). Lessons are still saved in the database.
        </p>
      )}
      {result && (
        <div className="mt-4 rounded-lg bg-bg-subtle p-3 text-sm">
          <p className="font-medium">
            {result.title}: {result.r.succeeded} ok{result.r.failed ? `, ${result.r.failed} failed` : ""}
          </p>
          {result.r.messages.length > 0 && (
            <ul className="mt-1 list-disc pl-5 text-fg-muted">
              {result.r.messages.map((m, i) => <li key={i}>{m}</li>)}
            </ul>
          )}
        </div>
      )}
      {error && <p className="mt-3 text-sm text-danger">{error}</p>}
    </section>
  );
}
