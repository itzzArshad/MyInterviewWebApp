"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { CloudOff, GitBranch, Loader2, RefreshCw } from "lucide-react";
import { api, ApiError } from "@/lib/api";
import type { SyncView } from "@/lib/types";

/** Shows whether this lesson is mirrored to GitHub, with a retry for failed syncs. */
export function SyncBadge({ slug, sync }: { slug: string; sync: SyncView }) {
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (sync.state === "DISABLED") return null;

  async function retry() {
    setBusy(true);
    setError(null);
    try {
      await api(`/lessons/${slug}/sync`, { method: "POST" });
      router.refresh();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Sync failed");
    } finally {
      setBusy(false);
    }
  }

  if (sync.state === "SYNCED") {
    return (
      <a
        href={sync.url ?? undefined}
        target="_blank"
        rel="noopener noreferrer"
        className="inline-flex items-center gap-1 text-xs text-fg-faint hover:text-fg"
        title={sync.path ?? undefined}
      >
        <GitBranch className="size-3.5" /> Saved to GitHub
      </a>
    );
  }

  return (
    <span className="inline-flex flex-wrap items-center gap-1.5 text-xs">
      <span className={clsx("inline-flex items-center gap-1", sync.state === "FAILED" ? "text-danger" : "text-warning")} title={sync.error ?? undefined}>
        <CloudOff className="size-3.5" />
        {sync.state === "FAILED" ? "GitHub sync failed" : "Not on GitHub yet"}
      </span>
      <button onClick={retry} disabled={busy} className="inline-flex items-center gap-1 text-fg-muted underline-offset-2 hover:underline">
        {busy ? <Loader2 className="size-3 animate-spin" /> : <RefreshCw className="size-3" />} Sync now
      </button>
      {(error ?? sync.error) && <span className="basis-full text-danger">{error ?? sync.error}</span>}
    </span>
  );
}
