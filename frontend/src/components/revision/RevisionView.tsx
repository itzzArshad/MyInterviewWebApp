"use client";

import { useRouter, useSearchParams } from "next/navigation";
import clsx from "clsx";
import { ReviewSession } from "./ReviewSession";
import { MistakeJournal } from "./MistakeJournal";
import { CheatSheets } from "./CheatSheets";

const TABS = [
  { key: "review", label: "Review" },
  { key: "mistakes", label: "Mistakes" },
  { key: "sheets", label: "Cheat sheets" },
] as const;

export function RevisionView() {
  const router = useRouter();
  const params = useSearchParams();
  const tab = (params.get("tab") as (typeof TABS)[number]["key"]) ?? "review";

  return (
    <>
      <h1 className="text-2xl font-bold tracking-tight md:text-3xl">Revision</h1>
      <p className="mt-1 text-fg-muted">Recall it before you forget it. Wrong answers come back tomorrow; right ones later and later.</p>
      <nav className="mt-6 flex gap-1 border-b border-border">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => router.replace(t.key === "review" ? "/revision" : `/revision?tab=${t.key}`)}
            className={clsx(
              "-mb-px border-b-2 px-3 py-2 text-sm transition",
              tab === t.key ? "border-fg font-medium text-fg" : "border-transparent text-fg-muted hover:text-fg",
            )}
          >
            {t.label}
          </button>
        ))}
      </nav>
      <div className="mt-6">
        {tab === "review" && <ReviewSession />}
        {tab === "mistakes" && <MistakeJournal />}
        {tab === "sheets" && <CheatSheets />}
      </div>
    </>
  );
}
