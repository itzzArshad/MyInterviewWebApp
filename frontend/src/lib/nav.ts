import {
  BookOpen,
  CalendarCheck,
  GitBranch,
  LayoutDashboard,
  NotebookPen,
  RotateCcw,
  Target,
  type LucideIcon,
} from "lucide-react";

export interface NavItem {
  key: string;
  label: string;
  href: string;
  icon: LucideIcon;
  /** Built and usable. Unavailable modules show a "coming in phase N" page. */
  available: boolean;
  phase: number;
  description: string;
  children?: { label: string; href: string }[];
}

export const NAV: NavItem[] = [
  {
    key: "today",
    label: "Today",
    href: "/today",
    icon: CalendarCheck,
    available: true,
    phase: 5,
    description: "Your daily training session: revise, learn, think, explain.",
  },
  {
    key: "dashboard",
    label: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
    available: true,
    phase: 1,
    description: "Progress, streak, readiness and what to study next.",
  },
  {
    key: "learn",
    label: "Learn",
    href: "/learn",
    icon: BookOpen,
    available: true,
    phase: 2,
    description:
      "Structured lessons from first principles — every concept with internals, code, pitfalls, interview Q&A and a quiz.",
    children: [
      { label: "Core Java", href: "/learn?subject=java" },
      { label: "Spring & Spring Boot", href: "/learn?subject=spring" },
      { label: "Backend Engineering", href: "/learn?subject=backend" },
      { label: "System Design", href: "/learn?subject=system-design" },
    ],
  },
  {
    key: "revision",
    label: "Revision",
    href: "/revision",
    icon: RotateCcw,
    available: true,
    phase: 5,
    description: "Spaced repetition of what you studied and what you got wrong — review it right before you forget it.",
  },
  {
    key: "interview",
    label: "Interview",
    href: "/interview",
    icon: Target,
    available: true,
    phase: 5,
    description: "Explain answers out loud, run timed mock interviews, and track readiness per area.",
  },
  {
    key: "notes",
    label: "Notes",
    href: "/notes",
    icon: NotebookPen,
    available: false,
    phase: 3,
    description:
      "A Notion-like workspace: nested pages, code blocks, tables — paste from ChatGPT and it stays formatted.",
  },
  {
    key: "github",
    label: "GitHub",
    href: "/github",
    icon: GitBranch,
    available: false,
    phase: 4,
    description: "Read-only sync of your repositories: Markdown and Java files rendered beautifully, updated on push.",
  },
];

export const MOBILE_TABS = ["today", "learn", "revision", "interview"];
