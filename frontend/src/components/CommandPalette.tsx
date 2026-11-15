"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import clsx from "clsx";
import { CornerDownLeft, Search } from "lucide-react";
import { NAV } from "@/lib/nav";

interface Entry {
  label: string;
  hint: string;
  href: string;
}

const ENTRIES: Entry[] = NAV.flatMap((n) => [
  { label: n.label, hint: "Go to", href: n.href },
  ...(n.children ?? []).map((c) => ({ label: c.label, hint: n.label, href: c.href })),
]);

/** Quick navigation (Ctrl/⌘+K). Phase 3 extends it to full-text search over notes and lessons. */
export function CommandPalette({ open, onClose }: { open: boolean; onClose: () => void }) {
  const router = useRouter();
  const [query, setQuery] = useState("");
  const [index, setIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const results = useMemo(() => {
    const q = query.trim().toLowerCase();
    return q ? ENTRIES.filter((e) => `${e.label} ${e.hint}`.toLowerCase().includes(q)) : ENTRIES;
  }, [query]);

  useEffect(() => {
    if (open) {
      setQuery("");
      setIndex(0);
      requestAnimationFrame(() => inputRef.current?.focus());
    }
  }, [open]);

  if (!open) return null;

  const go = (e: Entry | undefined) => {
    if (!e) return;
    onClose();
    router.push(e.href);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center bg-black/30 px-4 pt-[12vh]" onClick={onClose}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Search"
        onClick={(e) => e.stopPropagation()}
        className="w-full max-w-lg overflow-hidden rounded-xl border border-border bg-bg-elevated shadow-2xl"
      >
        <div className="flex items-center gap-2 border-b border-border px-4">
          <Search className="size-4 text-fg-faint" />
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => {
              setQuery(e.target.value);
              setIndex(0);
            }}
            onKeyDown={(e) => {
              if (e.key === "Escape") onClose();
              if (e.key === "ArrowDown") {
                e.preventDefault();
                setIndex((i) => Math.min(i + 1, results.length - 1));
              }
              if (e.key === "ArrowUp") {
                e.preventDefault();
                setIndex((i) => Math.max(i - 1, 0));
              }
              if (e.key === "Enter") go(results[index]);
            }}
            placeholder="Jump to…"
            className="h-12 flex-1 bg-transparent text-base outline-none placeholder:text-fg-faint sm:text-sm"
          />
        </div>
        <ul className="max-h-80 overflow-y-auto p-1.5">
          {results.length === 0 && <li className="px-3 py-6 text-center text-sm text-fg-faint">No results</li>}
          {results.map((e, i) => (
            <li key={e.href}>
              <button
                onMouseEnter={() => setIndex(i)}
                onClick={() => go(e)}
                className={clsx(
                  "flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm",
                  i === index ? "bg-bg-hover" : "",
                )}
              >
                <span className="flex-1">{e.label}</span>
                <span className="text-xs text-fg-faint">{e.hint}</span>
                {i === index && <CornerDownLeft className="size-3.5 text-fg-faint" />}
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}
