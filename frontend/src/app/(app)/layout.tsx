import { AppShell } from "@/components/AppShell";

// Server-rendered pages call the API; allow time for a free-tier backend that is waking up.
export const maxDuration = 60;

export default function AuthenticatedLayout({ children }: { children: React.ReactNode }) {
  return <AppShell>{children}</AppShell>;
}
