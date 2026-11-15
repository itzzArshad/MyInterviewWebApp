"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import clsx from "clsx";
import { ChevronsRight, Menu, Search, X } from "lucide-react";
import { MOBILE_TABS, NAV } from "@/lib/nav";
import { Sidebar } from "./Sidebar";
import { ThemeToggle } from "./ThemeToggle";
import { CommandPalette } from "./CommandPalette";
import { UserProvider } from "./UserContext";

const COLLAPSE_KEY = "academy.sidebarCollapsed";

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const [paletteOpen, setPaletteOpen] = useState(false);

  useEffect(() => {
    try {
      setCollapsed(localStorage.getItem(COLLAPSE_KEY) === "1");
    } catch {
      /* storage unavailable */
    }
  }, []);

  const toggleCollapsed = (value: boolean) => {
    setCollapsed(value);
    try {
      localStorage.setItem(COLLAPSE_KEY, value ? "1" : "0");
    } catch {
      /* storage unavailable */
    }
  };

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        setPaletteOpen((o) => !o);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  // Close the mobile drawer on route change.
  useEffect(() => setDrawerOpen(false), [pathname]);

  const current = NAV.find((n) => pathname === n.href || pathname.startsWith(n.href + "/"));

  return (
    <UserProvider>
      <div className="flex min-h-dvh">
        {/* Desktop sidebar */}
        <aside
          className={clsx(
            "sticky top-0 hidden h-dvh shrink-0 border-r border-border transition-[width] duration-200 md:block",
            collapsed ? "w-0 overflow-hidden border-r-0" : "w-60",
          )}
        >
          <Sidebar onOpenSearch={() => setPaletteOpen(true)} onCollapse={() => toggleCollapsed(true)} />
        </aside>

        {/* Mobile drawer */}
        <div
          className={clsx("fixed inset-0 z-40 md:hidden", drawerOpen ? "visible" : "invisible")}
          aria-hidden={!drawerOpen}
        >
          <div
            className={clsx("absolute inset-0 bg-black/30 transition-opacity", drawerOpen ? "opacity-100" : "opacity-0")}
            onClick={() => setDrawerOpen(false)}
          />
          <aside
            className={clsx(
              "absolute inset-y-0 left-0 w-72 max-w-[85vw] shadow-xl transition-transform duration-200",
              drawerOpen ? "translate-x-0" : "-translate-x-full",
            )}
          >
            <button
              onClick={() => setDrawerOpen(false)}
              aria-label="Close menu"
              className="absolute top-2.5 right-2 z-10 flex size-8 items-center justify-center rounded-md text-fg-muted hover:bg-bg-hover"
            >
              <X className="size-4" />
            </button>
            <Sidebar
              onNavigate={() => setDrawerOpen(false)}
              onOpenSearch={() => {
                setDrawerOpen(false);
                setPaletteOpen(true);
              }}
            />
          </aside>
        </div>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="sticky top-0 z-30 flex h-12 items-center gap-1 border-b border-border/60 bg-bg/85 px-2 backdrop-blur md:px-4">
            <button
              onClick={() => setDrawerOpen(true)}
              aria-label="Open menu"
              className="flex size-9 items-center justify-center rounded-md text-fg-muted hover:bg-bg-hover md:hidden"
            >
              <Menu className="size-5" />
            </button>
            {collapsed && (
              <button
                onClick={() => toggleCollapsed(false)}
                aria-label="Expand sidebar"
                className="hidden size-8 items-center justify-center rounded-md text-fg-muted hover:bg-bg-hover md:flex"
              >
                <ChevronsRight className="size-4" />
              </button>
            )}
            <span className="truncate px-1 text-sm text-fg-muted">{current?.label ?? ""}</span>
            <div className="flex-1" />
            <button
              onClick={() => setPaletteOpen(true)}
              aria-label="Search"
              className="flex size-9 items-center justify-center rounded-md text-fg-muted hover:bg-bg-hover hover:text-fg"
            >
              <Search className="size-[18px]" />
            </button>
            <ThemeToggle />
          </header>

          <main className="flex-1 pb-20 md:pb-0">{children}</main>
        </div>

        {/* Mobile bottom tab bar */}
        <nav
          aria-label="Quick"
          className="safe-bottom fixed inset-x-0 bottom-0 z-30 border-t border-border bg-bg/95 backdrop-blur md:hidden"
        >
          <ul className="grid grid-cols-4">
            {NAV.filter((n) => MOBILE_TABS.includes(n.key)).map((n) => {
              const active = current?.key === n.key;
              const Icon = n.icon;
              return (
                <li key={n.key}>
                  <Link
                    href={n.href}
                    className={clsx(
                      "flex h-14 flex-col items-center justify-center gap-0.5 text-[11px]",
                      active ? "text-fg" : "text-fg-faint",
                    )}
                  >
                    <Icon className="size-5" />
                    {n.label}
                  </Link>
                </li>
              );
            })}
          </ul>
        </nav>

        <CommandPalette open={paletteOpen} onClose={() => setPaletteOpen(false)} />
      </div>
    </UserProvider>
  );
}
