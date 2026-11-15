import Link from "next/link";
import type { Metadata } from "next";
import clsx from "clsx";
import { Bookmark, Clock } from "lucide-react";
import { serverApi } from "@/lib/server-api";
import type { PathNode } from "@/lib/types";
import { DifficultyBadge, MasteryPill, ProgressBar, StatusIcon } from "@/components/learn/badges";

export const metadata: Metadata = { title: "Learn" };

export default async function LearnPage({ searchParams }: { searchParams: Promise<{ subject?: string }> }) {
  const { subject: filter } = await searchParams;
  const tree = await serverApi<PathNode[]>("/learn/tree");
  const allSubjects = tree.flatMap((p) => p.subjects);
  const subjects = filter ? allSubjects.filter((s) => s.slug === filter) : allSubjects;

  const published = allSubjects.reduce((n, s) => n + s.published, 0);
  const completed = allSubjects.reduce((n, s) => n + s.completed, 0);
  const total = allSubjects.reduce((n, s) => n + s.total, 0);

  return (
    <div className="mx-auto max-w-4xl px-5 py-8 md:px-12 md:py-12">
      <h1 className="text-2xl font-bold tracking-tight md:text-3xl">{tree[0]?.title ?? "Learn"}</h1>
      <p className="mt-1 text-fg-muted">{tree[0]?.description}</p>
      <p className="mt-3 text-sm text-fg-faint">
        {completed} of {published} lessons completed · {total - published} more planned
      </p>

      {/* Subject filter */}
      <nav aria-label="Subjects" className="-mx-1 mt-6 flex gap-1 overflow-x-auto pb-1">
        <FilterTab href="/learn" active={!filter} label="All" />
        {allSubjects.map((s) => (
          <FilterTab key={s.slug} href={`/learn?subject=${s.slug}`} active={filter === s.slug} label={s.title} />
        ))}
      </nav>

      <div className="mt-8 space-y-14">
        {subjects.map((s) => {
          const pct = s.published ? Math.round((s.completed / s.published) * 100) : 0;
          return (
            <section key={s.slug} id={s.slug} className="scroll-mt-16">
              <div className="flex flex-wrap items-end justify-between gap-2">
                <div>
                  <h2 className="text-xl font-semibold tracking-tight">{s.title}</h2>
                  {s.description && <p className="mt-0.5 text-sm text-fg-muted">{s.description}</p>}
                </div>
                <span className="text-xs text-fg-faint">
                  {s.completed}/{s.published} done · {s.total} topics
                </span>
              </div>
              <ProgressBar value={pct} className="mt-3" />

              <div className="mt-6 space-y-6">
                {s.chapters.map((c, ci) => (
                  <div key={c.slug}>
                    <h3 className="mb-1.5 text-xs font-semibold tracking-wide text-fg-faint uppercase">
                      {ci + 1}. {c.title}
                    </h3>
                    <ul className="divide-y divide-border/60 rounded-lg border border-border">
                      {c.concepts.map((n) => (
                        <li key={n.slug}>
                          <Link
                            href={`/learn/${n.slug}`}
                            className={clsx(
                              "flex items-center gap-3 px-3 py-2.5 text-sm transition hover:bg-bg-subtle",
                              !n.published && "text-fg-faint",
                            )}
                          >
                            <StatusIcon status={n.progress} published={n.published} />
                            <span className="min-w-0 flex-1 truncate">{n.title}</span>
                            {n.bookmarked && <Bookmark className="size-3.5 shrink-0 fill-current text-warning" aria-label="Bookmarked" />}
                            {n.published ? (
                              <>
                                {n.quizzed && <MasteryPill value={n.mastery} />}
                                <DifficultyBadge value={n.difficulty} className="hidden sm:inline-flex" />
                                <span className="hidden w-14 items-center justify-end gap-1 text-xs text-fg-faint sm:flex">
                                  <Clock className="size-3" />
                                  {n.minutes}m
                                </span>
                              </>
                            ) : (
                              <span className="text-[11px]">Planned</span>
                            )}
                          </Link>
                        </li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
            </section>
          );
        })}
      </div>
    </div>
  );
}

function FilterTab({ href, active, label }: { href: string; active: boolean; label: string }) {
  return (
    <Link
      href={href}
      className={clsx(
        "shrink-0 rounded-md px-3 py-1.5 text-sm whitespace-nowrap transition",
        active ? "bg-fg text-bg" : "text-fg-muted hover:bg-bg-hover hover:text-fg",
      )}
    >
      {label}
    </Link>
  );
}
