"use client";

import { useEffect, useState } from "react";
import clsx from "clsx";
import type { TocItem } from "@/lib/toc";

/** Right-hand "On this page" navigation with scroll-spy. */
export function Toc({ items }: { items: TocItem[] }) {
  const [active, setActive] = useState<string | null>(items[0]?.id ?? null);

  useEffect(() => {
    const els = items.map((i) => document.getElementById(i.id)).filter((e): e is HTMLElement => !!e);
    const observer = new IntersectionObserver(
      (entries) => {
        const visible = entries.filter((e) => e.isIntersecting).sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top);
        if (visible[0]) setActive(visible[0].target.id);
      },
      { rootMargin: "-64px 0px -70% 0px" },
    );
    els.forEach((e) => observer.observe(e));
    return () => observer.disconnect();
  }, [items]);

  return (
    <nav aria-label="On this page" className="text-[13px]">
      <p className="mb-2 font-medium text-fg-muted">On this page</p>
      <ul className="space-y-1 border-l border-border">
        {items.map((i) => (
          <li key={i.id}>
            <a
              href={`#${i.id}`}
              className={clsx(
                "-ml-px block border-l py-0.5 pl-3 leading-snug transition",
                active === i.id ? "border-accent text-fg" : "border-transparent text-fg-faint hover:text-fg-muted",
              )}
            >
              {i.text.replace(/^\d+\.\s*/, "")}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
