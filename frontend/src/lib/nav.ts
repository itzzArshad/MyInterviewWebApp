import {
  BookOpen,
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
  /** Phase in which the module becomes available. */
  phase: number;
  description: string;
  children?: { label: string; href: string }[];
}

export const NAV: NavItem[] = [
  {
    key: "dashboard",
    label: "Dashboard",
    href: "/dashboard",
    icon: LayoutDashboard,
    phase: 1,
    description: "Progress, streak and what to study next.",
  },
  {
    key: "learn",
    label: "Learn",
    href: "/learn",
    icon: BookOpen,
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
    key: "notes",
    label: "Notes",
    href: "/notes",
    icon: NotebookPen,
    phase: 3,
    description:
      "A Notion-like workspace: nested pages, code blocks, tables — paste from ChatGPT and it stays formatted.",
  },
  {
    key: "github",
    label: "GitHub",
    href: "/github",
    icon: GitBranch,
    phase: 4,
    description: "Read-only sync of your repositories: Markdown and Java files rendered beautifully, updated on push.",
  },
  {
    key: "interview",
    label: "Interview",
    href: "/interview",
    icon: Target,
    phase: 5,
    description: "Timed assessments and mock interviews across Java, Spring, SQL, backend, LLD and HLD.",
  },
  {
    key: "revision",
    label: "Revision",
    href: "/revision",
    icon: RotateCcw,
    phase: 5,
    description: "Spaced repetition of concepts and questions you got wrong — review exactly what you are about to forget.",
  },
];

/** Modules that are built so far. Bump as phases ship. */
export const CURRENT_PHASE = 2;

export const MOBILE_TABS = ["dashboard", "learn", "notes", "revision"];
