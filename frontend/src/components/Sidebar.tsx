"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import clsx from "clsx";
import { ChevronRight, ChevronsLeft, LogOut, Search } from "lucide-react";
import { CURRENT_PHASE, NAV } from "@/lib/nav";
import { logout, useMe } from "./UserContext";

interface Props {
  onNavigate?: () => void;
  onOpenSearch: () => void;
  onCollapse?: () => void;
}

export function Sidebar({ onNavigate, onOpenSearch, onCollapse }: Props) {
  const pathname = usePathname();
  const me = useMe();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ learn: true });

  return (
    <nav aria-label="Main" className="flex h-full flex-col bg-bg-subtle text-sm">
      <div className="flex h-12 items-center justify-between px-3">
        <Link href="/dashboard" onClick={onNavigate} className="flex items-center gap-2 rounded-md px-1.5 py-1 hover:bg-bg-hover">
          <span className="flex size-6 items-center justify-center rounded-md bg-fg text-[11px] font-bold text-bg">BA</span>
          <span className="font-semibold">Backend Academy</span>
        </Link>
        {onCollapse && (
          <button
            onClick={onCollapse}
            aria-label="Collapse sidebar"
            className="flex size-7 items-center justify-center rounded-md text-fg-faint hover:bg-bg-hover hover:text-fg"
          >
            <ChevronsLeft className="size-4" />
          </button>
        )}
      </div>

      <div className="px-2 pb-2">
        <button
          onClick={onOpenSearch}
          className="flex w-full items-center gap-2 rounded-md px-2.5 py-1.5 text-fg-muted hover:bg-bg-hover"
        >
          <Search className="size-4" />
          <span className="flex-1 text-left">Search</span>
          <kbd className="hidden rounded border border-border px-1.5 text-[10px] text-fg-faint md:inline">Ctrl K</kbd>
        </button>
      </div>

      <ul className="flex-1 space-y-0.5 overflow-y-auto px-2">
        {NAV.map((item) => {
          const active = pathname === item.href || pathname.startsWith(item.href + "/");
          const available = item.phase <= CURRENT_PHASE;
          const isOpen = expanded[item.key];
          const Icon = item.icon;
          return (
            <li key={item.key}>
              <div
                className={clsx(
                  "group flex items-center rounded-md",
                  active ? "bg-bg-hover font-medium text-fg" : "text-fg-muted hover:bg-bg-hover hover:text-fg",
                )}
              >
                {item.children ? (
                  <button
                    onClick={() => setExpanded((s) => ({ ...s, [item.key]: !s[item.key] }))}
                    aria-label={isOpen ? `Collapse ${item.label}` : `Expand ${item.label}`}
                    aria-expanded={isOpen}
                    className="ml-1 flex size-6 items-center justify-center rounded text-fg-faint hover:bg-border"
                  >
                    <ChevronRight className={clsx("size-3.5 transition-transform", isOpen && "rotate-90")} />
                  </button>
                ) : (
                  <span className="ml-1 size-6" />
                )}
                <Link href={item.href} onClick={onNavigate} className="flex flex-1 items-center gap-2 py-1.5 pr-2.5">
                  <Icon className="size-4 shrink-0" />
                  <span className="flex-1 truncate">{item.label}</span>
                  {!available && (
                    <span className="rounded bg-border/70 px-1.5 py-px text-[10px] font-medium text-fg-faint">P{item.phase}</span>
                  )}
                </Link>
              </div>
              {item.children && isOpen && (
                <ul className="mt-0.5 ml-[34px] space-y-0.5 border-l border-border pl-2">
                  {item.children.map((c) => (
                    <li key={c.href}>
                      <Link
                        href={c.href}
                        onClick={onNavigate}
                        className="block truncate rounded-md px-2 py-1 text-fg-muted hover:bg-bg-hover hover:text-fg"
                      >
                        {c.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          );
        })}
      </ul>

      <div className="border-t border-border p-2">
        <div className="flex items-center gap-2 rounded-md px-2 py-1.5">
          <span className="flex size-7 items-center justify-center rounded-full bg-accent-soft text-xs font-semibold text-accent">
            {(me?.displayName ?? "?").slice(0, 1).toUpperCase()}
          </span>
          <span className="flex-1 truncate">{me?.displayName ?? "…"}</span>
          <button
            onClick={logout}
            aria-label="Log out"
            title="Log out"
            className="flex size-8 items-center justify-center rounded-md text-fg-faint hover:bg-bg-hover hover:text-danger"
          >
            <LogOut className="size-4" />
          </button>
        </div>
      </div>
    </nav>
  );
}
