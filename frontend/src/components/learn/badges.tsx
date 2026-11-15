import clsx from "clsx";
import { CheckCircle2, Circle, CircleDashed, CircleDot } from "lucide-react";
import type { Difficulty, ProgressStatus } from "@/lib/types";

const DIFFICULTY: Record<Difficulty, { label: string; cls: string }> = {
  BEGINNER: { label: "Beginner", cls: "bg-success/12 text-success" },
  INTERMEDIATE: { label: "Intermediate", cls: "bg-warning/15 text-warning" },
  ADVANCED: { label: "Advanced", cls: "bg-danger/12 text-danger" },
};

export function DifficultyBadge({ value, className }: { value: Difficulty; className?: string }) {
  const d = DIFFICULTY[value];
  return (
    <span className={clsx("inline-flex rounded px-1.5 py-0.5 text-[11px] font-medium", d.cls, className)}>
      {d.label}
    </span>
  );
}

export function StatusIcon({ status, published }: { status: ProgressStatus; published: boolean }) {
  if (!published) return <CircleDashed className="size-4 shrink-0 text-fg-faint/60" aria-label="Planned" />;
  if (status === "COMPLETED") return <CheckCircle2 className="size-4 shrink-0 text-success" aria-label="Completed" />;
  if (status === "IN_PROGRESS") return <CircleDot className="size-4 shrink-0 text-accent" aria-label="In progress" />;
  return <Circle className="size-4 shrink-0 text-fg-faint" aria-label="Not started" />;
}

export function MasteryPill({ value }: { value: number }) {
  const cls = value >= 80 ? "text-success" : value >= 60 ? "text-warning" : "text-danger";
  return <span className={clsx("text-xs font-medium tabular-nums", cls)}>{value}%</span>;
}

export function ProgressBar({ value, className }: { value: number; className?: string }) {
  return (
    <div className={clsx("h-1.5 overflow-hidden rounded-full bg-bg-hover", className)}>
      <div className="h-full rounded-full bg-accent transition-all" style={{ width: `${Math.min(100, Math.max(0, value))}%` }} />
    </div>
  );
}
