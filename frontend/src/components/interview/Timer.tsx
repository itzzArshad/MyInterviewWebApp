"use client";

import clsx from "clsx";
import { Timer as TimerIcon } from "lucide-react";

export function formatSeconds(s: number) {
  const m = Math.floor(s / 60);
  return `${m}:${String(s % 60).padStart(2, "0")}`;
}

/** Elapsed time vs. the suggested limit; turns red when you run over. */
export function Timer({ elapsed, limit }: { elapsed: number; limit: number }) {
  const over = elapsed > limit;
  const pct = Math.min(100, (elapsed / limit) * 100);
  return (
    <div className="min-w-32">
      <div className={clsx("flex items-center gap-1.5 font-mono text-sm tabular-nums", over ? "text-danger" : "text-fg-muted")}>
        <TimerIcon className="size-4" />
        {formatSeconds(elapsed)} <span className="text-fg-faint">/ {formatSeconds(limit)}</span>
      </div>
      <div className="mt-1 h-1 overflow-hidden rounded-full bg-bg-hover">
        <div className={clsx("h-full rounded-full transition-all", over ? "bg-danger" : "bg-accent")} style={{ width: `${pct}%` }} />
      </div>
    </div>
  );
}
