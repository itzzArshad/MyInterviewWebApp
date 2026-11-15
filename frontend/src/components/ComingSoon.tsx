import { NAV } from "@/lib/nav";

export function ComingSoon({ moduleKey }: { moduleKey: string }) {
  const item = NAV.find((n) => n.key === moduleKey);
  if (!item) return null;
  const Icon = item.icon;
  return (
    <div className="mx-auto max-w-3xl px-5 py-16 md:px-12 md:py-24">
      <Icon className="mb-5 size-10 text-fg-faint" strokeWidth={1.5} />
      <h1 className="text-3xl font-bold tracking-tight md:text-4xl">{item.label}</h1>
      <p className="mt-3 max-w-xl text-base leading-relaxed text-fg-muted">{item.description}</p>
      <div className="mt-8 inline-flex items-center gap-2 rounded-md border border-border bg-bg-subtle px-3 py-2 text-sm text-fg-muted">
        <span className="size-2 rounded-full bg-warning" />
        Arrives in Phase {item.phase}
      </div>
    </div>
  );
}
