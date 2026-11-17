"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import clsx from "clsx";
import { AlertTriangle, ArrowRight, Bookmark, BookOpen, Flame, Gauge, Loader2, RotateCcw, Target, TrendingUp } from "lucide-react";
import { api } from "@/lib/api";
import type { ConceptRef, Dashboard } from "@/lib/types";
import { NAV } from "@/lib/nav";
import { MasteryPill, ProgressBar } from "@/components/learn/badges";

function greeting() {
  const h = new Date().getHours();
  return h < 12 ? "Good morning" : h < 18 ? "Good afternoon" : "Good evening";
}

export function DashboardView() {
  const [data, setData] = useState<Dashboard | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Dashboard>("/dashboard")
      .then(setData)
      .catch((e: Error) => setError(e.message));
  }, []);

  if (error) {
    return <div className="mx-auto max-w-5xl px-5 py-10 text-sm text-danger md:px-12">Could not load dashboard: {error}</div>;
  }
  if (!data) {
    return (
      <div className="flex items-center justify-center py-32 text-fg-faint">
        <Loader2 className="size-5 animate-spin" />
      </div>
    );
  }

  const { stats, continueLearning: next } = data;
  const cards = [
    { label: "Overall progress", value: `${stats.overallProgressPct}%`, sub: `${stats.conceptsCompleted} / ${stats.conceptsTotal} lessons`, icon: TrendingUp },
    { label: "Streak", value: `${stats.streakDays} day${stats.streakDays === 1 ? "" : "s"}`, sub: "finished training sessions", icon: Flame },
    {
      label: "Quiz average",
      value: stats.quizAveragePct == null ? "—" : `${stats.quizAveragePct}%`,
      sub: stats.quizzesTaken ? `last ${stats.quizzesTaken} quiz${stats.quizzesTaken === 1 ? "" : "zes"}` : "No quizzes yet",
      icon: Gauge,
    },
    { label: "Interview readiness", value: `${stats.interviewReadinessPct}%`, sub: `${stats.dueForRevision} card${stats.dueForRevision === 1 ? "" : "s"} due for revision`, icon: Target },
  ];

  return (
    <div className="mx-auto max-w-5xl px-5 py-8 md:px-12 md:py-12">
      <h1 className="text-2xl font-bold tracking-tight md:text-3xl">
        {greeting()}, {data.displayName}
      </h1>
      <p className="mt-1 text-fg-muted">Pick up where you left off.</p>

      {/* Continue learning */}
      <Link
        href={next ? `/learn/${next.slug}` : "/learn"}
        className="group mt-8 flex items-center gap-4 rounded-xl border border-border p-4 transition hover:bg-bg-subtle md:p-5"
      >
        <span className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-accent-soft text-accent">
          <BookOpen className="size-5" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="block text-xs font-medium tracking-wide text-fg-faint uppercase">
            {next ? (next.started ? "Continue learning" : "Up next") : "All caught up"}
          </span>
          <span className="block truncate font-medium">{next ? next.title : "Every published lesson is complete 🎉"}</span>
          {next && (
            <span className="block truncate text-sm text-fg-muted">
              {next.subjectTitle} · {next.chapterTitle}
            </span>
          )}
        </span>
        <ArrowRight className="size-4 text-fg-faint transition group-hover:translate-x-0.5 group-hover:text-fg" />
      </Link>

      {/* Stats */}
      <section className="mt-6 grid grid-cols-2 gap-3 lg:grid-cols-4">
        {cards.map((c) => (
          <div key={c.label} className="rounded-xl border border-border p-4">
            <div className="flex items-center gap-1.5 text-xs text-fg-muted">
              <c.icon className="size-3.5" />
              {c.label}
            </div>
            <div className="mt-2 text-xl font-semibold tracking-tight md:text-2xl">{c.value}</div>
            <div className="mt-0.5 truncate text-xs text-fg-faint">{c.sub}</div>
          </div>
        ))}
      </section>

      <div className="mt-10 grid gap-10 lg:grid-cols-5">
        {/* Subjects */}
        <section className="lg:col-span-3">
          <h2 className="mb-4 text-sm font-semibold text-fg-muted">Subjects</h2>
          <ul className="space-y-4">
            {data.subjects.map((s) => {
              const pct = s.published ? Math.round((s.completed / s.published) * 100) : 0;
              return (
                <li key={s.slug}>
                  <Link href={`/learn?subject=${s.slug}`} className="group block">
                    <div className="mb-1.5 flex items-baseline justify-between text-sm">
                      <span className="font-medium group-hover:underline">{s.title}</span>
                      <span className="text-xs text-fg-faint">
                        {s.published ? `${s.completed}/${s.published} lessons` : `${s.total} topics planned`}
                      </span>
                    </div>
                    <ProgressBar value={pct} />
                  </Link>
                </li>
              );
            })}
          </ul>
        </section>

        {/* Activity */}
        <section className="lg:col-span-2">
          <h2 className="mb-4 text-sm font-semibold text-fg-muted">Activity · last 16 weeks</h2>
          <Heatmap activity={data.activity} />
        </section>
      </div>

      {/* Weak areas & bookmarks */}
      <div className="mt-10 grid gap-10 md:grid-cols-2">
        <ConceptList
          title="Weak areas"
          icon={<AlertTriangle className="size-4 text-warning" />}
          items={data.weakAreas}
          empty="Nothing weak yet — take some quizzes to find out."
          showMastery
        />
        <ConceptList
          title="Bookmarked for revision"
          icon={<Bookmark className="size-4 text-warning" />}
          items={data.bookmarks}
          empty="Bookmark difficult lessons to collect them here."
          showMastery={false}
        />
      </div>

      {/* Modules */}
      <section className="mt-12">
        <h2 className="mb-4 text-sm font-semibold text-fg-muted">Modules</h2>
        <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {data.modules.map((m) => {
            const nav = NAV.find((n) => n.key === m.key);
            const Icon = nav?.icon ?? RotateCcw;
            return (
              <li key={m.key}>
                <Link href={nav?.href ?? "/dashboard"} className="flex items-center gap-3 rounded-lg px-3 py-2.5 transition hover:bg-bg-subtle">
                  <Icon className="size-4 text-fg-muted" />
                  <span className="flex-1 text-sm">{m.title}</span>
                  <span
                    className={clsx(
                      "rounded px-1.5 py-0.5 text-[10px] font-medium",
                      m.available ? "bg-success/15 text-success" : "bg-bg-hover text-fg-faint",
                    )}
                  >
                    {m.available ? "Live" : `Phase ${m.phase}`}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      </section>
    </div>
  );
}

function ConceptList({
  title,
  icon,
  items,
  empty,
  showMastery,
}: {
  title: string;
  icon: React.ReactNode;
  items: ConceptRef[];
  empty: string;
  showMastery: boolean;
}) {
  return (
    <section>
      <h2 className="mb-3 flex items-center gap-2 text-sm font-semibold text-fg-muted">
        {icon}
        {title}
      </h2>
      {items.length === 0 ? (
        <p className="text-sm text-fg-faint">{empty}</p>
      ) : (
        <ul className="space-y-0.5">
          {items.map((c) => (
            <li key={c.slug}>
              <Link href={`/learn/${c.slug}`} className="flex items-center gap-3 rounded-md px-2 py-1.5 text-sm transition hover:bg-bg-subtle">
                <span className="flex-1 truncate">{c.title}</span>
                {showMastery && <MasteryPill value={c.mastery} />}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

/** GitHub-style 16×7 grid. Columns are weeks (oldest → newest), rows Mon→Sun. Strong = session finished. */
function Heatmap({ activity }: { activity: { date: string; completed: boolean }[] }) {
  const active = new Set(activity.map((d) => d.date));
  const done = new Set(activity.filter((d) => d.completed).map((d) => d.date));
  const today = new Date();
  const weeks = 16;
  const dow = (today.getDay() + 6) % 7; // 0 = Monday
  const start = new Date(today);
  start.setDate(today.getDate() - dow - (weeks - 1) * 7);

  const cols: { date: string; future: boolean; on: boolean; done: boolean }[][] = [];
  for (let w = 0; w < weeks; w++) {
    const col = [];
    for (let d = 0; d < 7; d++) {
      const dt = new Date(start);
      dt.setDate(start.getDate() + w * 7 + d);
      const iso = `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, "0")}-${String(dt.getDate()).padStart(2, "0")}`;
      col.push({ date: iso, future: dt > today, on: active.has(iso), done: done.has(iso) });
    }
    cols.push(col);
  }

  return (
    <div>
      <div className="flex gap-[3px]">
        {cols.map((col, i) => (
          <div key={i} className="flex flex-1 flex-col gap-[3px]">
            {col.map((c) => (
              <div
                key={c.date}
                title={c.future ? undefined : `${c.date}${c.done ? " · session completed" : c.on ? " · opened" : ""}`}
                className="aspect-square w-full rounded-[3px]"
                style={{
                  background: c.future ? "transparent" : c.done ? "var(--heat-1)" : c.on ? "var(--heat-half)" : "var(--heat-0)",
                }}
              />
            ))}
          </div>
        ))}
      </div>
      <p className="mt-2 text-xs text-fg-faint">
        {done.size} session{done.size === 1 ? "" : "s"} completed · {active.size} day{active.size === 1 ? "" : "s"} active
      </p>
    </div>
  );
}
